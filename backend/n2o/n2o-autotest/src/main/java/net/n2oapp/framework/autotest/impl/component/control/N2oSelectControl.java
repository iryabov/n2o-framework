package net.n2oapp.framework.autotest.impl.component.control;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.Condition;
import net.n2oapp.framework.autotest.api.component.control.SelectControl;
import org.openqa.selenium.Keys;

public class N2oSelectControl extends N2oControl implements SelectControl {
    @Override
    public void openOptions() {
        element().$(".n2o-input-control .n2o-popup-control").click();
    }

    @Override
    public void closeOptions() {

    }

    @Override
    public void find(String query) {
        element().$(".n2o-input-items input").sendKeys(query, Keys.ARROW_DOWN);
    }

    @Override
    public void select(int index) {
        element().parent().$$(".n2o-pop-up button").shouldBe(CollectionCondition.sizeGreaterThan(index)).get(index).click();
    }

    @Override
    public void select(Condition by) {
        element().parent().$$(".n2o-pop-up button").findBy(by).click();
    }

    @Override
    public void shouldSelected(String value) {
        element().$(".n2o-input-items").shouldHave(Condition.text(value));
    }

    @Override
    public void shouldBeDisabled() {
        element().shouldHave(Condition.cssClass("disabled"));
    }
}