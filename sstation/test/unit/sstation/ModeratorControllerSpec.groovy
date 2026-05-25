package sstation

import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@TestFor(ModeratorController)
@Mock(AcStudent)
class ModeratorControllerSpec extends Specification {

    def setup() {
    }

    def cleanup() {
    }

    void "test tabelStudentAdd can add student into list"() {
		when:"add Moderator to list"
//		controller._addModerator()=Mock()
		controller.index()
		then:"list should contain Moderator info"
		view=="/moderator/index"
    }
}