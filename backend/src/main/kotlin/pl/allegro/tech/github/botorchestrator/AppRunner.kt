package pl.allegro.tech.github.botorchestrator

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
class AppRunner

fun main(args: Array<String>) {
    SpringApplication.run(AppRunner::class.java, *args)
}
