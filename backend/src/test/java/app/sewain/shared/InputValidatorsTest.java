package app.sewain.shared;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class InputValidatorsTest {
 @Test void slugAllowsOptionalBlankAndLowercaseHyphenSeparatedWords(){var validator=new SlugValidator();for(String value:new String[]{null,""," ","tent","tent-4-person","123"})assertTrue(validator.isValid(value,null));for(String value:new String[]{"Tent","tent_4","-tent","tent-","tent--large","tent space","<script>"})assertFalse(validator.isValid(value,null));}
 @Test void phoneAcceptsInternationalFormatAndRemovesPresentationSeparators(){var validator=new PhoneNumberValidator();for(String value:new String[]{null,""," ","+628123456789","+62 812-3456-789"})assertTrue(validator.isValid(value,null));for(String value:new String[]{"08123456789","+0123456789","+123","+1234567890123456","+62abcdefghi"})assertFalse(validator.isValid(value,null));}
}
