package moe.kyokobot.koe.gradle;

import japicmp.model.JApiClass;
import japicmp.model.JApiCompatibility;
import japicmp.model.JApiMethod;
import me.champeau.gradle.japicmp.report.Violation;
import me.champeau.gradle.japicmp.report.stdrules.AbstractRecordingSeenMembers;

/**
 * Adding an abstract method is binary compatible, but it breaks consumers implementing or extending the type.
 * Such changes are only accepted on types annotated with {@code @ApiStatus.NonExtendable}, which are implemented
 * by Koe only.
 */
public class AbstractMethodAddedRule extends AbstractRecordingSeenMembers {
    // Has CLASS retention, so it isn't included in japicmp's annotation model and must be read from the class file.
    private static final String NON_EXTENDABLE = "org.jetbrains.annotations.ApiStatus$NonExtendable";

    @Override
    protected Violation maybeAddViolation(JApiCompatibility member) {
        JApiClass owner = null;
        if (member instanceof JApiMethod) {
            owner = ((JApiMethod) member).getjApiClass();
        } else if (member instanceof JApiClass) {
            owner = (JApiClass) member;
        }

        if (owner != null && owner.getNewClass().map(c -> c.hasAnnotation(NON_EXTENDABLE)).orElse(false)) {
            return Violation.accept(member, "Abstract method added to a @NonExtendable type");
        }

        return Violation.error(member, "Abstract method added to a type which can be implemented or extended "
                + "by users. Add a default (non-abstract) method instead, or annotate the type with "
                + "@ApiStatus.NonExtendable if only Koe implements it");
    }
}
