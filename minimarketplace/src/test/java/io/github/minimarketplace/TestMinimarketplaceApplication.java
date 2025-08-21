package io.github.minimarketplace;

import org.springframework.boot.SpringApplication;

public class TestMinimarketplaceApplication {

	public static void main(String[] args) {
		SpringApplication.from(MinimarketplaceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
