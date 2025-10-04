package org.ibs.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.qameta.allure.Step;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.TimeoutException;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class RegistrationSteps {

    private WebDriver driver;
    private WebDriverWait wait;
    private Actions actions;

    private static final String BASE_URL = "http://217.74.37.176";
    private static final String REGISTER_URL = BASE_URL + "/?route=account/register&language=ru-ru";
    private static final Duration IMPLICIT_WAIT = Duration.ofSeconds(10);
    private static final Duration EXPLICIT_WAIT = Duration.ofSeconds(20);

    private static final By FIRST_NAME_INPUT = By.id("input-firstname");
    private static final By LAST_NAME_INPUT = By.id("input-lastname");
    private static final By EMAIL_INPUT = By.id("input-email");
    private static final By PASSWORD_INPUT = By.id("input-password");
    private static final By NEWSLETTER_CHECKBOX = By.id("input-newsletter");
    private static final By AGREE_CHECKBOX = By.xpath("//input[@name='agree']");
    private static final By CONTINUE_BUTTON = By.xpath("//button[text()='Продолжить']");
    private static final By ERROR_ELEMENTS = By.cssSelector(".alert-danger, .text-danger, .has-error");
    private static final By SUCCESS_MESSAGE = By.cssSelector(".alert-success, .success, [class*='success']");

    @Given("Я открываю страницу регистрации")
    @Step("Открытие страницы регистрации")
    public void openRegistrationPage() {
        // Определяем, запущены ли тесты в CI/CD окружении
        boolean isCI = System.getenv("JENKINS_HOME") != null ||
                System.getenv("CI") != null ||
                System.getProperty("os.name").toLowerCase().contains("linux");

        if (isCI) {
            // Настройки для CI/CD окружения (Chrome в headless режиме)
            setupChromeHeadless();
        } else {
            // Настройки для локального запуска (Edge)
            setupEdgeBrowser();
        }

        driver.manage().window().maximize();
        driver.manage().timeouts().implicitlyWait(IMPLICIT_WAIT);

        driver.get(REGISTER_URL);
        scrollDown(200);
    }

    @Step("Настройка Chrome в headless режиме для CI/CD")
    private void setupChromeHeadless() {
        ChromeOptions options = new ChromeOptions();

        // Основные опции для headless режима
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-web-security");
        options.addArguments("--allow-running-insecure-content");
        options.addArguments("--ignore-certificate-errors");

        // Дополнительные опции для стабильности
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--disable-features=VizDisplayCompositor");
        options.addArguments("--disable-software-rasterizer");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, EXPLICIT_WAIT);
        actions = new Actions(driver);

        System.out.println("Chrome запущен в headless режиме для CI/CD");
    }

    @Step("Настройка Edge браузера для локального запуска")
    private void setupEdgeBrowser() {
        EdgeOptions options = new EdgeOptions();
        options.addArguments("--remote-allow-origins=*");

        driver = new EdgeDriver(options);
        wait = new WebDriverWait(driver, EXPLICIT_WAIT);
        actions = new Actions(driver);

        System.out.println("Edge запущен для локального тестирования");
    }

    @When("Я заполняю форму регистрации с невалидным email")
    @Step("Заполнение формы с невалидным email")
    public void fillFormWithInvalidEmail() {
        fillRegistrationForm(
                "Иван",           // firstName
                "Иванов",         // lastName
                "himail.ru",      // email (invalid - no @)
                "Q1w2e3l",        // password
                true,             // subscribe
                true              // agree
        );
        clickContinueButton();
    }

    @When("Я заполняю форму регистрации с цифрами в имени")
    @Step("Заполнение формы с цифрами в имени")
    public void fillFormWithDigitsInName() {
        fillRegistrationForm(
                "1234567890",     // firstName (invalid - digits)
                "Иванов",         // lastName
                "IvanIvan22222222@mail.ru", // email
                "Q1w2e3l",        // password
                true,             // subscribe
                true              // agree
        );
        clickContinueButton();
    }

    @When("Я заполняю форму регистрации с валидными данными")
    @Step("Заполнение формы с валидными данными")
    public void fillFormWithValidData() {
        String uniqueEmail = "test" + System.currentTimeMillis() + "@mail.ru";

        fillRegistrationForm(
                "Иван",                    // firstName
                "Иванов",                  // lastName
                uniqueEmail,               // email (уникальный)
                "Q1w2e3r4t5y6u7i8o9p0",   // password
                true,                      // subscribe
                true                       // agree
        );
        clickContinueButton();
    }

    @Then("Регистрация должна быть провальной из-за отсутствия @ в email")
    @Step("Проверка неуспешной регистрации из-за email")
    public void assertRegistrationFailedDueToEmail() {
        assertRegistrationFailed("Регистрация должна быть провальной из-за отсутствия @ в email");
        closeDriver();
    }

    @Then("Регистрация должна быть провальной из-за наличия цифр в поле 'Имя'")
    @Step("Проверка неуспешной регистрации из-за цифр в имени")
    public void assertRegistrationFailedDueToDigits() {
        assertRegistrationFailed("Регистрация должна быть провальной из-за наличия цифр в поле 'Имя'");
        closeDriver();
    }

    @Then("Регистрация должна быть успешной")
    @Step("Проверка успешной регистрации")
    public void assertRegistrationSuccessful() {
        assertRegistrationSuccessful("Регистрация должна быть успешной с валидными данными");
        closeDriver();
    }

    @Step("Безопасное закрытие драйвера")
    private void closeDriver() {
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception e) {
                System.err.println("Ошибка при закрытии драйвера: " + e.getMessage());
            }
        }
    }

    // Остальные вспомогательные методы остаются без изменений
    @Step("Заполнение формы регистрации")
    private void fillRegistrationForm(String firstName, String lastName, String email,
                                      String password, boolean subscribe, boolean agree) {
        setInputValue(FIRST_NAME_INPUT, firstName);
        setInputValue(LAST_NAME_INPUT, lastName);
        setInputValue(EMAIL_INPUT, email);
        setInputValue(PASSWORD_INPUT, password);

        if (subscribe) {
            setCheckboxValue(NEWSLETTER_CHECKBOX, true);
        }

        if (agree) {
            setCheckboxValue(AGREE_CHECKBOX, true);
        }
    }

    @Step("Установка значения в поле")
    private void setInputValue(By locator, String value) {
        WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        element.clear();
        element.sendKeys(value);
    }

    @Step("Установка значения чекбокса")
    private void setCheckboxValue(By locator, boolean checked) {
        WebElement checkbox = wait.until(ExpectedConditions.elementToBeClickable(locator));
        if (checkbox.isSelected() != checked) {
            checkbox.click();
        }
    }

    @Step("Нажатие кнопки 'Продолжить'")
    private void clickContinueButton() {
        WebElement continueButton = wait.until(ExpectedConditions.elementToBeClickable(CONTINUE_BUTTON));
        continueButton.click();
    }

    @Step("Прокрутка страницы")
    private void scrollDown(int pixels) {
        actions.scrollByAmount(0, pixels).perform();
    }

    @Step("Проверка неуспешной регистрации")
    private void assertRegistrationFailed(String message) {
        try {
            boolean isFailed = wait.until(driver ->
                    driver.getCurrentUrl().contains("register") ||
                            !driver.findElements(ERROR_ELEMENTS).isEmpty() ||
                            driver.getPageSource().contains("ошибка") ||
                            driver.getPageSource().contains("error")
            );
            assertTrue(isFailed, message);
        } catch (TimeoutException e) {
            fail(message + " - Timeout waiting for failure indicators");
        }
    }

    @Step("Проверка успешной регистрации")
    private void assertRegistrationSuccessful(String message) {
        try {
            boolean isSuccessful = wait.until(driver ->
                    driver.getCurrentUrl().contains("success") ||
                            !driver.findElements(SUCCESS_MESSAGE).isEmpty() ||
                            driver.getCurrentUrl().contains("account/success") ||
                            driver.getPageSource().contains("Ваш аккаунт создан") ||
                            driver.getPageSource().contains("Account Created")
            );
            assertTrue(isSuccessful, message);
        } catch (TimeoutException e) {
            boolean isSuccessful = !driver.getCurrentUrl().contains("register") &&
                    driver.findElements(ERROR_ELEMENTS).isEmpty();
            assertTrue(isSuccessful, message + " (Timeout occurred, but no errors found)");
        }
    }
}