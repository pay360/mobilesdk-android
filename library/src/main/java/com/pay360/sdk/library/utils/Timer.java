/*
 * Copyright (c) 2016 Capita plc
 */

package com.pay360.sdk.library.utils;

import android.os.Handler;

/**
 * Wraps Handler.postDelayed() into a nicer Timer model
 */
public class Timer {

	private long timeoutMillis;
	private boolean repeating;
	private Handler handler;
	private Runnable taskToRun;
	private Runnable taskToRunInternal;
	
	/**
	 * Creates  Timer object, the context parameter is not used, and this
	 * constructor has been left for backward compatibility.
	 * @param taskToRun The Runnable task the timer executes.
	 * @param timeoutMillis The delay before taskToRun is executed.
	 * @param repeating True if the task repeats, false otherwise.
	 */
	public Timer(Runnable taskToRun, long timeoutMillis, boolean repeating) {
		this.timeoutMillis = timeoutMillis;
		this.repeating = repeating;
		this.handler = new Handler();
		this.taskToRun = taskToRun;
		
		taskToRunInternal = new Runnable() {
			
			@Override
			public void run() {
				// kick off external task to run if set
				if (Timer.this.taskToRun != null) {
					Timer.this.taskToRun.run();
				}
				
				if (Timer.this.repeating) {
					// repeat timer
					handler.postDelayed(taskToRunInternal, Timer.this.timeoutMillis);
				}
			}
		};
	}
	
	/** Starts the timer - you should typically call this in onStart() or onResume()
	 */
	public void start() {
		// cancel current
		reset(timeoutMillis);
	}

    /** Starts the timer - you should typically call this in onStart() or onResume()
     */
    public void start(long timeoutMillis) {
        // cancel current
        reset(timeoutMillis);
    }

    public void reset() {
        reset(timeoutMillis);
    }
	
	private void reset(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;

		// cancel current
		cancel();
		
		if (this.taskToRun != null) {
			handler.postDelayed(taskToRunInternal, timeoutMillis);
		}
	}
	
	/**
	 * Stops the timer - you should typically call this in onStop() or onPause()
	 */
	public void cancel() {
		handler.removeCallbacks(taskToRunInternal);
	}
}
