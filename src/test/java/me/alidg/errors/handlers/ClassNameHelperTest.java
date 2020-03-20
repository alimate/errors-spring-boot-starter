package me.alidg.errors.handlers;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class ClassNameHelperTest {
    @Test
    public void getClassName_topLevelClass() {
        Assertions.assertThat(ClassNameHelper.getClassName(Enclosing.class)).isEqualTo("Enclosing");
    }

    @Test
    public void getClassName_nestedClass() {
        Assertions.assertThat(ClassNameHelper.getClassName(Enclosing.Nested.class)).isEqualTo("Enclosing.Nested");
    }

    @Test
    public void getClassName_innerClass() {
        Assertions.assertThat(ClassNameHelper.getClassName(Enclosing.Inner.class)).isEqualTo("Enclosing.Inner");
    }

    @Test
    public void getClassName_innerInsideNestedClass() {
        Assertions.assertThat(ClassNameHelper.getClassName(Enclosing.Nested.Inner.class))
            .isEqualTo("Enclosing.Nested.Inner");
    }

    @Test
    public void getClassName_innerInsideInnerClass() {
        Assertions.assertThat(ClassNameHelper.getClassName(Enclosing.Inner.Innermost.class))
            .isEqualTo("Enclosing.Inner.Innermost");
    }
}

class Enclosing {
    static class Nested {
        class Inner {}
    }

    class Inner {
        class Innermost {}
    }
}

