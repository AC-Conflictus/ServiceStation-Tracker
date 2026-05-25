package sstation

import grails.test.mixin.TestFor
import spock.lang.Specification
import grails.plugin.springsecurity.SpringSecurityService
import grails.plugin.springsecurity.SpringSecurityUtils

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@TestFor(HomeController)
@Mock([AcUser, AcRole, AcUserAcRole, AcStudent, ServiceHour,HourService,SpringSecurityService])
class HomeControllerSpec extends Specification {

    def setup() {
    }

    def cleanup() {
    }

	/*
	*/
	
	
	void "test that non admin user redirects to student home"() {
		given:
		controller.hourService=Mock(HourService)
		
		def mockSecurityService = Mock(SpringSecurityService)

		def adminRole = new AcRole(authority: "ROLE_ADMIN").save(flush:true)
		def studentRole = new AcRole(authority: "ROLE_STUDENT").save(flush:true)
		def moderatorRole = new AcRole(authority: "ROLE_MODERATOR").save(flush:true)
	
		def testAdminUser = new AcUser(username:'admin', password: 'admin_secret').save(flush:true)
		def testStudentUser = new AcUser(username:'student', password: 'student_secret').save(flush:true)
		def testModeratorUser = new AcUser(username:'moderator', password: 'moderator_secret').save(flush:true)
		
		AcUserAcRole.create(testAdminUser, adminRole, true)
		AcUserAcRole.create(testStudentUser, studentRole, true)
		AcUserAcRole.create(testModeratorUser, moderatorRole, true)
	
		SpringSecurityUtils.metaClass.'static'.ifAllGranted = { String role ->
			return true
		}
		
		def user = testAdminUser
		controller.springSecurityService = [principal: user]
		
		
				
		when:"index method is called"
		controller.index()
		
		then:"view should be index"
		response.redirectedUrl == '/acStudent/home/1'
	}
	
	
	
	
	void "test that admin user redirects to admin home"() {
		given:
		controller.hourService=Mock(HourService)
		
		def mockSecurityService = Mock(SpringSecurityService)

		def adminRole = new AcRole(authority: "ROLE_ADMIN").save(flush:true)
		def studentRole = new AcRole(authority: "ROLE_STUDENT").save(flush:true)
		def moderatorRole = new AcRole(authority: "ROLE_MODERATOR").save(flush:true)
	
		def testAdminUser = new AcUser(username:'admin', password: 'admin_secret').save(flush:true)
		def testStudentUser = new AcUser(username:'student', password: 'student_secret').save(flush:true)
		def testModeratorUser = new AcUser(username:'moderator', password: 'moderator_secret').save(flush:true)
		
		AcUserAcRole.create(testAdminUser, adminRole, true)
		AcUserAcRole.create(testStudentUser, studentRole, true)
		AcUserAcRole.create(testModeratorUser, moderatorRole, true)
	
		SpringSecurityUtils.metaClass.'static'.ifAllGranted = { String role ->
			return false
		}
		
		def user = testAdminUser
		controller.springSecurityService = [principal: user]
		
		
				
		when:"index method is called"
		controller.index()
		
		then:"view should be index"
		response.redirectedUrl == '/home/adminHome'
	}
}