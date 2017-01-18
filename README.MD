开发一个多进程应用的时候，我们往往无法避免在多个进程之间共享数据。
多进程共享数据的方法有很多种，在Android中常用的有：SharedPreferences(多进程模式)、广播、Socket、ContentProvider、Messenger、AIDL等。这些方法适用于不同的使用场景，又有各自的局限性。

本文即将介绍的是通过ContentProvider，结合SharedPreferences(以下简称SP)实现的进程间共享设置项的功能。这种方式主要适用于以下场景：在一个进程中进行一些设置，而需要在另一个进程实时读取设置，并根据这些设置来执行功能。

###1.SharedPreferences在多进程共享下的局限
有些同学可能会觉得奇怪：明明上面才说了SP(多进程模式)也是多进程共享数据的方法，为什么还需要通过ContentProvider来做呢。答案很简单，***因为SP其实并不能保证多进程间同步***，下图是关于MODE_MULTI_PROCESS的注释，我就不全文翻译了。

![MODE_MULTI_PROCESS模式的注释](http://upload-images.jianshu.io/upload_images/2270777-ea40421e2e5c46db.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

我们都知道，SP其实是将key-value对保存在手机的data/data/you.package.name/shared_prefs/目录下的文件中。为了减少IO造成的性能损失，SP使用了缓存的机制，会先把数据保存在内存中，在读取的时候直接从内存中读取，而写的时候才会保存到文件。也就是说普通SP是一次读取，多次写入的工作模式。所以如果多个进程中都使用了普通的SP，分别进行保存就会导致相互覆盖。而设置了MODE_MULTI_PROCESS之后，在多进程使用的时候，会在检测到文件变化的时候重新加载文件到内存中，这样虽然损失了一部分性能，但是却***部分***实现了多进程间同步。

为什么说是"部分"实现了多进程间同步呢？因为在频繁进行SP操作的时候，就还是会出现相互覆盖的问题。我初步估计是因为两个进程同时进行了文件的写操作，带着这个猜测我阅读了源码。

先看看对MODE_MULTI_PROCESS是如何处理的
***ContextImpl的getSharedPreferences方法：***
```
@Override
public SharedPreferences getSharedPreferences(File file, int mode) {
	checkMode(mode);
	SharedPreferencesImpl sp;
	synchronized (ContextImpl.class) {
		final ArrayMap<File, SharedPreferencesImpl> cache = getSharedPreferencesCacheLocked();
		sp = cache.get(file);
		if (sp == null) {
			sp = new SharedPreferencesImpl(file, mode);
			cache.put(file, sp);
			return sp;
		}
	}
	if ((mode & Context.MODE_MULTI_PROCESS) != 0 ||
		getApplicationInfo().targetSdkVersion < android.os.Build.VERSION_CODES.HONEYCOMB) {
		// If somebody else (some other process) changed the prefs
		// file behind our back, we reload it.  This has been the
		// historical (if undocumented) behavior.
		sp.startReloadIfChangedUnexpectedly();
	}
	return sp;
}
```
***其中SharedPreferencesImpl的startReloadIfChangedUnexpectedly方法：***
```
    void startReloadIfChangedUnexpectedly() {
        synchronized (this) {
            // TODO: wait for any pending writes to disk?
            if (!hasFileChangedUnexpectedly()) {
                return;
            }
            startLoadFromDisk();
        }
    }
```
可见遇到MODE_MULTI_PROCESS的时候，会强制让SP进行一次读取操作，从而保证数据是最新的。因此如果你在外部保存了一份SP的对象，反而会导致享受不到MODE_MULTI_PROCESS带来的同步效果了，而且从源码中可以看出ContextImpl中是对SP对象做了缓存的，每次重新getSharedPreferences并不会造成太大的性能损失。
从getSharedPreferences的代码中看不出会造成多进程互相覆盖的问题，那我们看看Editor的commit方法，
***EditorImpl的commit和相关方法***
```
final class SharedPreferencesImpl implements SharedPreferences {
	...
	public final class EditorImpl implements Editor {
		...
		private MemoryCommitResult commitToMemory() {
			...
		}
		public boolean commit() {
			MemoryCommitResult mcr = commitToMemory();
			SharedPreferencesImpl.this.enqueueDiskWrite(
				mcr, null /* sync write on this thread okay */);
			try {
				mcr.writtenToDiskLatch.await();
			} catch (InterruptedException e) {
				return false;
			}
			notifyListeners(mcr);
			return mcr.writeToDiskResult;
		}
	}

	private void enqueueDiskWrite(final MemoryCommitResult mcr,
								  final Runnable postWriteRunnable) {
		final Runnable writeToDiskRunnable = new Runnable() {
				public void run() {
					synchronized (mWritingToDiskLock) {
						writeToFile(mcr);
					}
					synchronized (SharedPreferencesImpl.this) {
						mDiskWritesInFlight--;
					}
					if (postWriteRunnable != null) {
						postWriteRunnable.run();
					}
				}
			};

		final boolean isFromSyncCommit = (postWriteRunnable == null);

		// Typical #commit() path with fewer allocations, doing a write on
		// the current thread.
		if (isFromSyncCommit) {
			boolean wasEmpty = false;
			synchronized (SharedPreferencesImpl.this) {
				wasEmpty = mDiskWritesInFlight == 1;
			}
			if (wasEmpty) {
				writeToDiskRunnable.run();
				return;
			}
		}

		QueuedWork.singleThreadExecutor().execute(writeToDiskRunnable);
	}
}
```
可见每次会先保存到内存，然后再写入文件，而写文件的操作又是在子线程中依次按顺序执行的(上面代码最后一行)。
那么结论就出来了：

***当多个进程同时而又高频的调用commit方法时，就会导致文件被反复覆盖写入，而并没有被及时读取，所以造成进程间数据的不同步***

###2.通过ContentProvider进行实现多进程共享SharedPreferences

既然SP有多进程不同步的隐患，那么我们怎么要怎么解决呢？
多进程同步的方法中，ContentProvider、Messenger、AIDL等方式都是基于Binder实现的，所以本质上并没有太大差别，而ContentProvider又是Android提倡的数据提供组件，所以我选择它来实现多进程SP操作。

***具体怎么做呢？***
SP本身的调用方式已经提供了较高的存取便利性，所以我们只要封装出一个SPHelper，去调用SPContentProvider，SPContentProvider用于保证跨进程的同步性，其内部再用SPHelperImpl来做真正的实现即可。


![对SP的封装结构](http://upload-images.jianshu.io/upload_images/2270777-9aa7922da65a2754.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

那怎么用SPContentProvider来实现数据存取操作呢？实现ContentProvider需要实现几个方法，这些方法分别对应了ContentResolver中的同名方法，我们可以通过ContentResolver来调用这些方法，达到传递数据和进行命令解析的目的。
![ContentProvider需要实现的方法](http://upload-images.jianshu.io/upload_images/2270777-4757dc8d2cc767b5.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

SP中的方法有sava，get，remove，clean，getAll这几种。那么我们可以用update或insert来实现save；用delete实现clean和remove，用getType或者query实现get和getAll。
所以最终的调用方式如下如所示：

![SP调用getInt()的传递过程](http://upload-images.jianshu.io/upload_images/2270777-331190f68e726c76.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

下面看看代码中是如何处理的，还是以getInt()为例：
```
public class SPHelper {
	...

    public static final String CONTENT="content://";
    public static final String AUTHORITY="com.pl.sphelper";
    public static final String SEPARATOR= "/";
    public static final String CONTENT_URI =CONTENT+AUTHORITY;
    public static final String TYPE_INT="int";
	public static final String NULL_STRING= "null";

	public static int getInt(String name, int defaultValue) {
        ContentResolver cr = context.getContentResolver();
        Uri uri = Uri.parse(CONTENT_URI + SEPARATOR + TYPE_INT + SEPARATOR + name);
        String rtn = cr.getType(uri);
        if (rtn == null || rtn.equals(NULL_STRING)) {
            return defaultValue;
        }
        return Integer.parseInt(rtn);
    }
	...
}

public class SPContentProvider extends ContentProvider{
	...
    public static final String SEPARATOR= "/";

	public String getType(Uri uri) {
        // 用这个来取数值
        String[] path= uri.getPath().split(SEPARATOR);
        String type=path[1];
        String key=path[2];
        return  ""+SPHelperImpl.get(getContext(),key,type);
    }
	...
}

class SPHelperImpl {
	...

    public static final String TYPE_INT="int";

    static String get(Context context, String name, String type) {
        if (type.equalsIgnoreCase(TYPE_STRING)) {
			return getString(context, name, null);
		} else if (type.equalsIgnoreCase(TYPE_BOOLEAN)) {
			return getBoolean(context, name, false);
		} else if (type.equalsIgnoreCase(TYPE_INT)) {
			return getInt(context, name, 0);
		} else if (type.equalsIgnoreCase(TYPE_LONG)) {
			return getLong(context, name, 0L);
		} else if (type.equalsIgnoreCase(TYPE_FLOAT)) {
			return getFloat(context, name, 0f);
		} else if (type.equalsIgnoreCase(TYPE_STRING_SET)) {
			return getString(context, name, null);
		}
		return null;
    }

    static int getInt(Context context, String name, int defaultValue) {
        SharedPreferences sp = getSP(context);
        if (sp == null) return defaultValue;
        return sp.getInt(name, defaultValue);
    }
	...
}
```
其中SPContentProvider必须在AndroidManifest中声明，并设置android:authorities="com.pl.sphelper"。
###3.优化性能和内存
本来以为做完上面的工作就算完了，但是在实际使用的过程中，发现一个问题，就是内存消耗比较大。tracking后发现是由于生成的SharedPreferences.Editor对象占用了大量内存，这是因为我的应用场景中，会频繁的将运行过程中的几个数据存储到SP中，所以导致大量生成Editor对象。但是实际上，很多保存的值是相同的，此时可以考虑使用缓存机制，而不用重复写入，代码如下：

```

private static SoftReference<Map<String, Object>> sCacheMap;

private static Object getCachedValue(String name) {
	if (sCacheMap != null) {
		Map<String, Object> map = sCacheMap.get();
		if (map != null) {
			return map.get(name);
		}
	}
	return null;
}

private static void setValueToCached(String name, Object value) {
	Map<String, Object> map;
	if (sCacheMap == null) {
		map = new HashMap<>();
		sCacheMap = new SoftReference<Map<String, Object>>(map);
	} else {
		map = sCacheMap.get();
		if (map == null) {
			map = new HashMap<>();
			sCacheMap = new SoftReference<Map<String, Object>>(map);
		}
	}
	map.put(name, value);
}
synchronized static <T> void save(Context context, String name, T t) {
	SharedPreferences sp = getSP(context);
	if (sp == null) return;

	if (t.equals(getCachedValue(name))) {
		return;
	}
	SharedPreferences.Editor editor = sp.edit();
	if (t instanceof Boolean) {
		editor.putBoolean(name, (Boolean) t);
	}
	if (t instanceof String) {
		editor.putString(name, (String) t);
	}
	if (t instanceof Integer) {
		editor.putInt(name, (Integer) t);
	}
	if (t instanceof Long) {
		editor.putLong(name, (Long) t);
	}
	if (t instanceof Float) {
		editor.putFloat(name, (Float) t);
	}
	editor.commit();
	setValueToCached(name, t);
}
```
实测内存使用量下降80%（这么大比例的原因有一部分是因为我一原先消耗的内存太多了），改进效果还是比较明显的。

###4.还没有完，SPHelper会造成多大的性能损失呢？
既然是多进程间的交互，肯定会造成一定的性能损失，那么具体是多少呢？我用以下代码测试了一下：
```
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() throws Exception {
		Context appContext = InstrumentationRegistry.getTargetContext();
		SPHelper.init((Application) appContext.getApplicationContext());
		Random random = new Random();
		long start = System.currentTimeMillis();
		for (int i = 0; i < 100; i++) {
			SPHelper.save("key" + random.nextInt(200), i);
		}
		long end = System.currentTimeMillis();
		Log.e("ExampleInstrumentedTest", "SPHelper takes " + (end - start) + "millis");

		start = System.currentTimeMillis();
		for (int i = 0; i < 100; i++) {
			SharedPreferences sp = appContext.getSharedPreferences("text", Context.MODE_PRIVATE);
			SharedPreferences.Editor editor = sp.edit();
			editor.putInt("key" + random.nextInt(200), i);
			editor.commit();
		}
		end = System.currentTimeMillis();
		Log.e("ExampleInstrumentedTest", "SharedPreferences takes " + (end - start) + "millis");
    }
}
```
代码中是SPHelper和系统默认的SharedPreferences ，进行了100次的SP储存操作，输出消耗的时间（单位：毫秒）。

当调用方和SPContentProvider在***相同***的进程中时，性能如下：

 | 调用方式 | 第一次 | 第二次 | 第三次 | 第四次 | 第五次 |
 |:--------:|:-----:|:----:|:----:|:----:|:----:|
 | SPHelper | 966 | 903 | 944 | 987 | 951 |
 | SharedPreferences | 850 | 836 | 904 | 844 | 838 |
 | 性能差距 | **0.14** | **0.08** | **0.04** | **0.17** | **0.13** |

***可见性能损失并不大，大概10%左右***。这是因为Binder的工作方式，当在两者同一个进程中是，只是相当于函数调用，不会引起太大的消耗，具体的可以阅读Binder源码。所以这10%的差距基本上都是由于对数据的包装和解包装所产生的。

那么进程间通信的消耗到底有多大呢，下表是调用方和SPContentProvider在***不同***的进程中时的结果：

| 调用方式 | 第一次 | 第二次 | 第三次 | 第四次 | 第五次 |
|:--------:|:-----:|:----:|:----:|:----:|:----:|
| SPHelper | 1374 | 1283 | 1366 | 1286 | 1296 |
| SharedPreferences | 829 | 850 | 861 | 886 | 869 |
| 性能差距 | **0.66** | **0.51** | **0.59** | **0.45** | **0.49** |

***可见通过三次函数就行转发调用，再加上进程间通信，造成的性能损失还是比较可观的，达到50%以上。***

######需要注意的是，如果SPContentProvider所在的进程只有这个ContentProvider而没有其他组件的话，第一次调用时才会启动进程，所以会耗费大约50ms以上的时间，而且调用完以后一段时间，这个进程就会自动消耗，所以使用的时候，最好把SPContentProvider放到最常用SP的进程中，这样才能保证性能消耗最小。

