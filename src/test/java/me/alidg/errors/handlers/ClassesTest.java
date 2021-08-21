package me.alidg.errors.handlers;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class ClassesTest {

    @Test
    public void getClassName_topLevelClass() {
        Assertions.assertThat(Classes.getClassName(Enclosing.class)).isEqualTo("Enclosing");
    }

    @Test
    public void getClassName_nestedClass() {
        Assertions.assertThat(Classes.getClassName(Enclosing.Nested.class)).isEqualTo("Enclosing.Nested");
    }

    @Test
    public void getClassName_innerClass() {
        Assertions.assertThat(Classes.getClassName(Enclosing.Inner.class)).isEqualTo("Enclosing.Inner");
    }

    @Test
    public void getClassName_innerInsideNestedClass() {
        Assertions.assertThat(Classes.getClassName(Enclosing.Nested.Inner.class))
            .isEqualTo("Enclosing.Nested.Inner");
    }

    @Test
    public void getClassName_innerInsideInnerClass() {
        Assertions.assertThat(Classes.getClassName(Enclosing.Inner.Innermost.class))
            .isEqualTo("Enclosing.Inner.Innermost");
    }
}

@SuppressWarnings("InnerClassMayBeStatic")
class Enclosing {

    static class Nested {
        class Inner {
        }
    }

    class Inner {
        class Innermost {
        }
    }
}