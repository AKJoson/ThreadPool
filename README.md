## Android 线程池
###线程池是有状态的，为了保存这种状态，FrameWork的工程师用了个int来存储，同时为了记录执行的线程数，牛x的工程师用一个字段来表示这个运行状态以及运行的线程数。
* 下面就是那几个比较重要的标志：<br>
* 
		//保证原子性，AtomicInteger 
		private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));
		// 32 - 3 = 29 
	    private static final int COUNT_BITS = Integer.SIZE - 3; 
		//1左移29位 就变成了 100000（一共30位），后面减1的话，就变成了29个1咯。
	    private static final int CAPACITY   = (1 << COUNT_BITS) - 1;
	
	    // runState is stored in the high-order bits  高三位表示的是运行状态
	    private static final int RUNNING    = -1 << COUNT_BITS;  // 这个是 111000..()
	    private static final int SHUTDOWN   =  0 << COUNT_BITS;
	    private static final int STOP       =  1 << COUNT_BITS;
	    private static final int TIDYING    =  2 << COUNT_BITS;
	    private static final int TERMINATED =  3 << COUNT_BITS;

	    // Packing and unpacking ctl
		// ~CAPACITY 这个按位取反操作，让高3位变成了1，低29位之前是1，取反之后变成了0
	    private static int runStateOf(int c)     { return c & ~CAPACITY; }//过滤掉低29的干扰，只要高3位的状态。
		//CAPACITY的高三位全是0 低29位全是1，因此c&CAPICITY也就是取出低29位的值，过滤掉高3位的干扰。
	    private static int workerCountOf(int c)  { return c & CAPACITY; } //取出当前所有的线程数
	    private static int ctlOf(int rs, int wc) { return rs | wc; }

### ThreadPoolExecutor.java
	    public void execute(Runnable command) {
        if (command == null)
            throw new NullPointerException();
		## 系统工程师写的注释就像贴心小棉袄一样暖
		## 万般千种，怎得一个详细了得。
		# step1-->少于核心线程，搞一个线程直接运行起来
		# step2-->双重锁保护起，安全的将runable加入队列.
		# step3-->又不能运行，也不能加入队列，只能说一声 Darling,good bye.
        /*
         * Proceed in 3 steps:
         *
         * 1. If fewer than corePoolSize threads are running, try to
         * start a new thread with the given command as its first
         * task.  The call to addWorker atomically checks runState and
         * workerCount, and so prevents false alarms that would add
         * threads when it shouldn't, by returning false.
         *
         * 2. If a task can be successfully queued, then we still need
         * to double-check whether we should have added a thread
         * (because existing ones died since last checking) or that
         * the pool shut down since entry into this method. So we
         * recheck state and if necessary roll back the enqueuing if
         * stopped, or start a new thread if there are none.
         *
         * 3. If we cannot queue task, then we try to add a new
         * thread.  If it fails, we know we are shut down or saturated
         * and so reject the task.
         */
        int c = ctl.get();
        if (workerCountOf(c) < corePoolSize) { //当前正在running的线程，是否小于核心线程
            if (addWorker(command, true))      //addWorker(runnable) 开始愉快的运行吧。
                return;
            c = ctl.get(); //添加核心失败，再次获取状态
        }
        if (isRunning(c) && workQueue.offer(command)) { //线程池状态是运行状态。从上面到这说明立刻运行核心，没成功，那么接来时添加进入队列
            int recheck = ctl.get();
            if (! isRunning(recheck) && remove(command))// 虽然添加进了队列，但是线程池的状态已经变成了停止
                reject(command);						//执行 AbortPolicy 默认策略，抛出这个异常。
            else if (workerCountOf(recheck) == 0) //已经停止 , 运行的线程数0 
                addWorker(null, false); 	//不明白，这个是搞什么的。	
        }
        else if (!addWorker(command, false)) //加入队列也不行了，可能时因为队列满了，那么将其搞成Worker来执行起哦。
            reject(command);
    }

###瞧一瞧addWorkder()这个方法，隐藏着什么猫腻...
	
  	private boolean addWorker(Runnable firstTask, boolean core) {
        retry:
        for (;;) {
            int c = ctl.get();
            int rs = runStateOf(c);

            // Check if queue empty only if necessary.
            if (rs >= SHUTDOWN &&
                ! (rs == SHUTDOWN &&
                   firstTask == null &&			//非running的状态，给我返回false就是了
                   ! workQueue.isEmpty()))	
                return false;

            for (;;) {
                int wc = workerCountOf(c);		//取出当前的线程数，其实这个workerCountOf取出的最大值吧就是29个1
                if (wc >= CAPACITY ||			//当前的线程数已经超出了29个1
                    wc >= (core ? corePoolSize : maximumPoolSize)) //如果是核心线程是否已经超过核心数，或者非核心，是否超过最大？
                    return false;
                if (compareAndIncrementWorkerCount(c))	//开始增加1，设置线程数
                    break retry;
                c = ctl.get();  // Re-read ctl  //纳里？ 增加不了？
                if (runStateOf(c) != rs)		// 刚刚进入的状态和现在不同，1 2 3 4 再来一次。
                    continue retry;
                // else CAS failed due to workerCount change; retry inner loop
            }
        }

        boolean workerStarted = false;	//开始 标记准备好，要开始搞事情了。
        boolean workerAdded = false;	//添加 标记
        Worker w = null;
        try {
            w = new Worker(firstTask);  //将我们的task封装成Worker,Worker包装着一个Runnbale并且在在构造器里面，创建一个Thread。 一对一关系。
            final Thread t = w.thread; // Worker的构造函数里面创建了一个新的线程。
            if (t != null) {
                final ReentrantLock mainLock = this.mainLock; 
                mainLock.lock();		//重入锁的大写锁定开启，every 巴蒂，准备搞事情。
                try {
                    // Recheck while holding lock.
                    // Back out on ThreadFactory failure or if
                    // shut down before lock acquired.
                    int rs = runStateOf(ctl.get()); //再次获取运行状态

                    if (rs < SHUTDOWN ||						//只有 running的-1才小于SHUTDOWN的0
                        (rs == SHUTDOWN && firstTask == null)) {
                        if (t.isAlive()) // precheck that t is startable //准备搞事情的这个线程是不是-->started
                            throw new IllegalThreadStateException();
                        workers.add(w); //这个workers是一个HasSet<Worker> 加入集合。
                        int s = workers.size(); //集合的size().
                        if (s > largestPoolSize) // largestPoolSize根本没进行初始化，默认值是0 
                            largestPoolSize = s;//所以这没添加一个，就加1个。可以认为这个值表示着当前的线程一共有多少个。
                        workerAdded = true; // 一切顺利，我们的Work创建好了，并且也成功的添加到了Set集合。
                    }
                } finally {
                    mainLock.unlock(); //存储集合不是线程安全的，因此到这个就算添加完毕了。好的，现在给我解锁。
                }
                if (workerAdded) { //添加完成之后，紧随其后的就是 Thread.start() --> 给我进入执行状态
                    t.start();
                    workerStarted = true;  
                }
            }
        } finally {
            if (! workerStarted)
                addWorkerFailed(w); //添加失败了，从Set中移除这个work.
        }
        return workerStarted;
    }


