package net.corda.server

import org.springframework.boot.Banner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer
import org.springframework.web.socket.server.jetty.JettyRequestUpgradeStrategy
import org.springframework.web.socket.server.standard.TomcatRequestUpgradeStrategy
import org.springframework.web.socket.server.support.DefaultHandshakeHandler
import springfox.documentation.swagger2.annotations.EnableSwagger2
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket




@SpringBootApplication
@ComponentScan(basePackages = [
	"net.corda.server",
	"com.r3.corda.finance.cash.issuer.service.api.controllers"
])
@EnableSwagger2
class Starter {
	// When false, Tomcat is used
	companion object {
		val USE_JETTY = false
	}

	/** Registers an endpoint for STOMP messages. */
	@Configuration
	/** Registers an endpoint for STOMP messages. */
	@EnableWebSocketMessageBroker
	open class WebSocketConfig : WebSocketMessageBrokerConfigurer {
		override fun registerStompEndpoints(registry: StompEndpointRegistry) {
			registry.addEndpoint("/stomp").setHandshakeHandler(handshakeHandler()).withSockJS()
		//			.setStreamBytesLimit(512 * 1024)
		//			.setHttpMessageCacheSize(1000)
					.setDisconnectDelay(600 * 1000)
					.setSupressCors(true)
		}

		fun handshakeHandler(): DefaultHandshakeHandler {
			val handler = if (USE_JETTY)
				DefaultHandshakeHandler(JettyRequestUpgradeStrategy())
			else
				DefaultHandshakeHandler(TomcatRequestUpgradeStrategy())

			return handler
		}
/*
		override fun configureMessageBroker(registry: MessageBrokerRegistry) {
			registry.setApplicationDestinationPrefixes("/app")
			registry.enableSimpleBroker("/topic", "/queue")
		}
*/
	}

	@Configuration
	@EnableSwagger2
	inner class SwaggerConfig {
		@Bean
		fun api(): Docket {
			return Docket(DocumentationType.SWAGGER_2)
					.select()
					.apis(RequestHandlerSelectors.any())
					.paths(PathSelectors.any())
					.build()
		}
	}

}

fun main(args: Array<String>) {
	//runApplication<Starter>(*args)
	val app = SpringApplication(Starter::class.java)
	app.setBannerMode(Banner.Mode.OFF)
	//app.isWebEnvironment = true
	app.run(*args)
}

