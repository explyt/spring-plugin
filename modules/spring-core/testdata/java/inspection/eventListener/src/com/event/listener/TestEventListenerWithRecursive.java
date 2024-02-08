package com.event.listener;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class TestEventListenerWithRecursive {

	@EventListener
	public @interface CustomEventListener {
	}

	@CustomEventListener
	void test0() {
	}

	@CustomEventListener
	protected void test1() {
	}

	@CustomEventListener
	private void test2() {
	}

	@CustomEventListener
	public void test3() {
	}

	@CustomEventListener
	public void test4(String param1, String param2) {
	}

	@CustomEventListener
	private void test5(String param1, String param2) {
	}

	@CustomEventListener
	public void test6(String param1) {
	}
}
