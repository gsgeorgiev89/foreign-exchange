package com.example.fx;

import org.springframework.boot.SpringApplication;

public class TestFxApplication {

	public static void main(String[] args) {
		SpringApplication.from(FxApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
