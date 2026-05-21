package com.platform.proxy.core.utils;

import com.platform.proxy.command.querytranslator.domain.FilterCondition;
import com.platform.proxy.command.querytranslator.domain.FilterGroup;
import com.platform.proxy.command.querytranslator.domain.FilterOperator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FilterStringTokenizerTest {

    private final FilterStringTokenizer tokenizer = new FilterStringTokenizer();

    @Test
    void parsesSingleCondition() {
        FilterGroup group = tokenizer.tokenizeGroup("spec.type=image");

        assertThat(group.getConditions()).hasSize(1);
        FilterCondition c = group.getConditions().get(0);
        assertThat(c.getField()).isEqualTo("spec.type");
        assertThat(c.getOperator()).isEqualTo(FilterOperator.EQ);
        assertThat(c.getValue()).isEqualTo("image");
    }

    @Test
    void parsesMultipleAndConditionsPreservingNestedFields() {
        FilterGroup group = tokenizer.tokenizeGroup("spec.type=image,spec.appId=CLAUT");

        assertThat(group.getConditions())
                .extracting(FilterCondition::getField)
                .containsExactly("spec.type", "spec.appId");
    }

    @Test
    void parsesComparisonOperators() {
        assertThat(tokenizer.tokenizeCondition("count>=5").getOperator()).isEqualTo(FilterOperator.GTE);
        assertThat(tokenizer.tokenizeCondition("count<=5").getOperator()).isEqualTo(FilterOperator.LTE);
        assertThat(tokenizer.tokenizeCondition("count>5").getOperator()).isEqualTo(FilterOperator.GT);
        assertThat(tokenizer.tokenizeCondition("count<5").getOperator()).isEqualTo(FilterOperator.LT);
        assertThat(tokenizer.tokenizeCondition("state!=down").getOperator()).isEqualTo(FilterOperator.NEQ);
    }

    @Test
    void parsesLikeAndInMarkers() {
        FilterCondition like = tokenizer.tokenizeCondition("metadata.name=~web");
        assertThat(like.getOperator()).isEqualTo(FilterOperator.LIKE);
        assertThat(like.getValue()).isEqualTo("web");

        FilterCondition in = tokenizer.tokenizeCondition("spec.type=in(image|service)");
        assertThat(in.getOperator()).isEqualTo(FilterOperator.IN);
        assertThat(in.getValue()).isEqualTo("image|service");
    }

    @Test
    void rejectsBlankFilter() {
        assertThatThrownBy(() -> tokenizer.tokenizeGroup("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsConditionWithoutOperator() {
        assertThatThrownBy(() -> tokenizer.tokenizeCondition("justafield"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsEmptyValue() {
        assertThatThrownBy(() -> tokenizer.tokenizeCondition("spec.type="))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
