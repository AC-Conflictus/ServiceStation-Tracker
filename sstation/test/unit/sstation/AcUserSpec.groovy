package sstation

import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.domain.DomainClassUnitTestMixin} for usage instructions
 */
@TestFor(AcUser)
class AcUserSpec extends Specification {

	void "encodePassword throws instead of storing plaintext when springSecurityService is unwired"() {
		given: "a user with no springSecurityService injected (mimics the early-boot wiring gap)"
		def user = new AcUser(username: "nobody", password: "secret")

		when:
		user.encodePassword()

		then:
		def e = thrown(IllegalStateException)
		e.message.contains("springSecurityService")
		user.password == "secret" // untouched — definitely never silently re-stored as plaintext
	}

	void "encodePassword delegates to springSecurityService when wired"() {
		given:
		def user = new AcUser(username: "someone", password: "secret")
		user.springSecurityService = [encodePassword: { String p -> "ENC(${p})".toString() }]

		when:
		user.encodePassword()

		then:
		user.password == "ENC(secret)"
	}
}
