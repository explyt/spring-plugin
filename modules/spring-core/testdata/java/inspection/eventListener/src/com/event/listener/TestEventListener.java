package com.event.listener;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class TestEventListener {
	@EventListener
	void test0() {
	}

	@EventListener
	protected void test1() {
	}

	@EventListener
	private void test2() {
	}

	@EventListener
	public void test3() {
	}

	@EventListener
	public void test4(String param1, String param2) {
	}

	@EventListener
	private void test5(String param1, String param2) {
	}

	@EventListener
	public void test6(String param1) {
	}
}
