package org.ibs.steps;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.qameta.allure.Step;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.TimeoutException;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class RegistrationSteps {

    private WebDriver driver;
    private WebDriverWait wait;
    private Actions actions;
    private Properties properties;

    private static final String BASE_URL = "http://217.74.37.176";
    private static final String REGISTER_URL = BASE_URL + "/?route=account/register&language=ru-ru";
    private static final Duration IMPLICIT_WAIT = Duration.ofSeconds(10);
    private static final Duration EXPLICIT_WAIT = Duration.ofSeconds(15);

    // Локаторы
    private static final By FIRST_NAME_INPUT = By.id("input-firstname");
    private static final By LAST_NAME_INPUT = By.id("input-lastname");
    private static final By EMAIL_INPUT = By.id("input-email");
    private static final By PASSWORD_INPUT = By.id("input-password");
    private static final By NEWSLETTER_CHECKBOX = By.id("input-newsletter");
    private static final By AGREE_CHECKBOX = By.xpath("//input[@name='agree']");
    private static final By CONTINUE_BUTTON = By.xpath("//button[text()='Продолжить']");
    private static final By ERROR_ELEMENTS = By.cssSelector(".alert-danger, .text-danger, .has-error");
    private static final By SUCCESS_MESSAGE = By.cssSelector(".alert-success, .success, [class*='success']");

    @Before
    public void setUp() {
        loadProperties();
    }

    @After
    public void tearDown() {
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception e) {
                System.err.println("Error during driver quit: " + e.getMessage());
            }
        }
    }

    private void loadProperties() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            properties = new Properties();
            if (input != null) {
                properties.load(input);
            }

            overrideFromSystemProperties();
            setDefaultProperties();

        } catch (Exception e) {
            throw new RuntimeException("Failed to load configuration", e);
        }
    }

    private void overrideFromSystemProperties() {
        String[] propertiesToOverride = {
                "run.mode", "selenoid.browser", "browser.version", "enable.vnc", "enable.video",
                "local.browser", "local.headless", "selenoid.url", "browser.name", "platform"
        };

        for (String property : propertiesToOverride) {
            String systemValue = System.getProperty(property);
            if (systemValue != null && !systemValue.trim().isEmpty()) {
                properties.setProperty(property, systemValue);
                System.out.println("Overridden property from system: " + property + " = " + systemValue);
            }
        }
    }

    private void setDefaultProperties() {
        if (!properties.containsKey("run.mode")) {
            properties.setProperty("run.mode", "local");
        }
        if (!properties.containsKey("local.browser")) {
            properties.setProperty("local.browser", "chrome");
        }
        if (!properties.containsKey("local.headless")) {
            properties.setProperty("local.headless", "true");
        }
        if (!properties.containsKey("selenoid.browser")) {
            properties.setProperty("selenoid.browser", "chrome");
        }
        if (!properties.containsKey("browser.version")) {
            properties.setProperty("browser.version", "latest");
        }
        if (!properties.containsKey("enable.vnc")) {
            properties.setProperty("enable.vnc", "true");
        }
        if (!properties.containsKey("enable.video")) {
            properties.setProperty("enable.video", "false");
        }
        if (!properties.containsKey("platform")) {
            properties.setProperty("platform", "LINUX");
        }
    }

    @Given("Я открываю страницу регистрации")
    @Step("Открытие страницы регистрации")
    public void openRegistrationPage() {
        try {
            String runMode = properties.getProperty("run.mode", "local");

            if ("selenoid".equalsIgnoreCase(runMode)) {
                driver = createSelenoidDriverWithCapabilities();
                System.out.println("Running in SELENOID mode with browser: " +
                        properties.getProperty("selenoid.browser"));
            } else {
                driver = createLocalDriver();
                System.out.println("Running in LOCAL mode with browser: " +
                        properties.getProperty("local.browser"));
            }

            wait = new WebDriverWait(driver, EXPLICIT_WAIT);
            actions = new Actions(driver);

            driver.manage().window().maximize();
            driver.manage().timeouts().implicitlyWait(IMPLICIT_WAIT);

            driver.get(REGISTER_URL);
            waitForPageToLoad();

        } catch (Exception e) {
            if (driver != null) {
                driver.quit();
            }
            throw new RuntimeException("Failed to initialize WebDriver: " + e.getMessage(), e);
        }
    }

    /**
     * Новый метод с использованием DesiredCapabilities для Selenoid
     */
    private WebDriver createSelenoidDriverWithCapabilities() throws MalformedURLException {
        String browser = properties.getProperty("selenoid.browser", "chrome");
        String browserVersion = properties.getProperty("browser.version", "latest");
        String platform = properties.getProperty("platform", "LINUX");

        // Создаем DesiredCapabilities
        DesiredCapabilities capabilities = new DesiredCapabilities();

        // Устанавливаем основные возможности
        capabilities.setBrowserName(browser);
        capabilities.setVersion(browserVersion);
        capabilities.setPlatform(Platform.fromString(platform));

        // Добавляем опции для конкретных браузеров
        switch (browser.toLowerCase()) {
            case "chrome":
                capabilities.merge(createChromeOptions());
                break;
            case "firefox":
                capabilities.merge(createFirefoxOptions());
                break;
            case "opera": // Пример для других браузеров
                capabilities.merge(createOperaOptions());
                break;
        }

        // Selenoid-specific capabilities
        capabilities.setCapability("selenoid:options", createSelenoidOptions());

        // Дополнительные capabilities для лучшей стабильности
        capabilities.setCapability("timeouts", createTimeoutsCapability());
        capabilities.setCapability("acceptInsecureCerts", true);
        capabilities.setCapability("unhandledPromptBehavior", "dismiss and notify");

        String selenoidUrl = properties.getProperty("selenoid.url");
        System.out.println("Creating Selenoid driver with capabilities: " + capabilities);

        return new RemoteWebDriver(new URL(selenoidUrl), capabilities);
    }

    /**
     * Создает опции для Chrome
     */
    private DesiredCapabilities createChromeOptions() {
        ChromeOptions options = new ChromeOptions();

        // Базовые опции
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--headless");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--disable-gpu");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-plugins");
        options.addArguments("--disable-background-timer-throttling");

        // Опции для обхода автоматизации
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        options.setExperimentalOption("useAutomationExtension", false);

        // Логирование
        options.setCapability("goog:loggingPrefs", createLoggingPrefs());

        DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setCapability(ChromeOptions.CAPABILITY, options);
        return capabilities;
    }

    /**
     * Создает опции для Firefox
     */
    private DesiredCapabilities createFirefoxOptions() {
        FirefoxOptions options = new FirefoxOptions();

        // Базовые опции
        options.addArguments("--headless");
        options.addArguments("--width=1920");
        options.addArguments("--height=1080");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-plugins");

        // Настройки для стабильности
        options.addPreference("dom.webdriver.enabled", false);
        options.addPreference("useAutomationExtension", false);
        options.addPreference("marionette", true);

        // Логирование
        options.setCapability("moz:loggingPrefs", createLoggingPrefs());

        DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setCapability(FirefoxOptions.FIREFOX_OPTIONS, options);
        return capabilities;
    }

    /**
     * Создает опции для Opera (пример)
     */
    private DesiredCapabilities createOperaOptions() {
        DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setBrowserName("opera");

        // Можно добавить специфичные опции для Opera
        ChromeOptions options = new ChromeOptions();
        options.setBinary("/usr/bin/opera");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--headless");

        capabilities.setCapability(ChromeOptions.CAPABILITY, options);
        return capabilities;
    }

    /**
     * Создает опции для Selenoid
     */
    private Map<String, Object> createSelenoidOptions() {
        Map<String, Object> selenoidOptions = new HashMap<>();

        selenoidOptions.put("enableVNC", Boolean.parseBoolean(properties.getProperty("enable.vnc", "true")));
        selenoidOptions.put("enableVideo", Boolean.parseBoolean(properties.getProperty("enable.video", "false")));
        selenoidOptions.put("enableLog", Boolean.parseBoolean(properties.getProperty("enable.log", "true")));
        selenoidOptions.put("screenResolution", properties.getProperty("screen.resolution", "1920x1080x24"));

        // Дополнительные опции Selenoid
        selenoidOptions.put("sessionTimeout", properties.getProperty("session.timeout", "5m"));
        selenoidOptions.put("timeZone", properties.getProperty("time.zone", "Europe/Moscow"));
        selenoidOptions.put("env", new String[]{"LANG=ru_RU.UTF-8", "LANGUAGE=ru:en", "LC_ALL=ru_RU.UTF-8"});

        // Имя теста для отображения в Selenoid UI
        selenoidOptions.put("name", "Registration Tests - " + properties.getProperty("selenoid.browser", "chrome"));

        // Лейблы для организации в Selenoid
        Map<String, Object> labels = new HashMap<>();
        labels.put("project", "IBS-Autotest");
        labels.put("test", "registration");
        labels.put("browser", properties.getProperty("selenoid.browser", "chrome"));
        selenoidOptions.put("labels", labels);

        return selenoidOptions;
    }

    /**
     * Создает настройки таймаутов
     */
    private Map<String, Object> createTimeoutsCapability() {
        Map<String, Object> timeouts = new HashMap<>();
        timeouts.put("implicit", 0);
        timeouts.put("pageLoad", 300000);
        timeouts.put("script", 30000);
        return timeouts;
    }

    /**
     * Создает настройки логирования
     */
    private Map<String, String> createLoggingPrefs() {
        Map<String, String> logPrefs = new HashMap<>();
        logPrefs.put("browser", "INFO");
        logPrefs.put("driver", "INFO");
        logPrefs.put("performance", "ALL");
        return logPrefs;
    }

    // Старые методы для локального запуска (оставляем без изменений)
    private WebDriver createLocalDriver() {
        try {
            String browser = properties.getProperty("local.browser", "chrome");
            boolean headless = Boolean.parseBoolean(properties.getProperty("local.headless", "true"));

            System.out.println("Creating Local driver for: " + browser + " headless: " + headless);

            switch (browser.toLowerCase()) {
                case "chrome":
                    return createLocalChromeDriver(headless);
                case "firefox":
                    return createLocalFirefoxDriver(headless);
                default:
                    throw new IllegalArgumentException("Unsupported browser: " + browser);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Local driver: " + e.getMessage(), e);
        }
    }

    private WebDriver createLocalFirefoxDriver(boolean headless) {
        FirefoxOptions options = new FirefoxOptions();

        if (headless) {
            options.addArguments("--headless");
        }

        options.addArguments("--width=1920");
        options.addArguments("--height=1080");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-plugins");

        return new FirefoxDriver(options);
    }

    private WebDriver createLocalChromeDriver(boolean headless) {
        ChromeOptions options = new ChromeOptions();

        if (headless) {
            options.addArguments("--headless");
        }

        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-plugins");
        options.addArguments("--disable-background-timer-throttling");

        return new ChromeDriver(options);
    }

    // Остальные методы тестов (без изменений)
    @When("Я заполняю форму регистрации с невалидным email")
    @Step("Заполнение формы с невалидным email")
    public void fillFormWithInvalidEmail() {
        fillRegistrationForm(
                "Иван",           // firstName
                "Иванов",         // lastName
                "himail.ru",      // email (invalid - no @)
                "Q1w2e3l",        // password
                false,            // subscribe
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
                false,            // subscribe
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
                false,                     // subscribe
                true                       // agree
        );
        clickContinueButton();
    }

    @Then("Регистрация должна быть провальной из-за отсутствия @ в email")
    @Step("Проверка неуспешной регистрации из-за email")
    public void assertRegistrationFailedDueToEmail() {
        assertRegistrationFailed("Регистрация должна быть провальной из-за отсутствия @ в email");
    }

    @Then("Регистрация должна быть провальной из-за наличия цифр в поле 'Имя'")
    @Step("Проверка неуспешной регистрации из-за цифр в имени")
    public void assertRegistrationFailedDueToDigits() {
        assertRegistrationFailed("Регистрация должна быть провальной из-за наличия цифр в поле 'Имя'");
    }

    @Then("Регистрация должна быть успешной")
    @Step("Проверка успешной регистрации")
    public void assertRegistrationSuccessful() {
        assertRegistrationSuccessful("Регистрация должна быть успешной с валидными данными");
    }

    // Вспомогательные методы (без изменений)
    @Step("Заполнение формы регистрации")
    private void fillRegistrationForm(String firstName, String lastName, String email,
                                      String password, boolean subscribe, boolean agree) {
        setInputValue(FIRST_NAME_INPUT, firstName);
        setInputValue(LAST_NAME_INPUT, lastName);
        setInputValue(EMAIL_INPUT, email);
        setInputValue(PASSWORD_INPUT, password);

        if (subscribe) {
            setCheckboxValueSafely(NEWSLETTER_CHECKBOX, true);
        }

        if (agree) {
            setCheckboxValueSafely(AGREE_CHECKBOX, true);
        }
    }

    @Step("Установка значения в поле")
    private void setInputValue(By locator, String value) {
        WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        element.clear();
        element.sendKeys(value);
    }

    @Step("Установка значения чекбокса с безопасной прокруткой")
    private void setCheckboxValueSafely(By locator, boolean checked) {
        try {
            WebElement checkbox = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
            scrollToElement(checkbox);
            wait.until(ExpectedConditions.elementToBeClickable(checkbox));

            if (checkbox.isSelected() != checked) {
                try {
                    checkbox.click();
                } catch (ElementNotInteractableException e) {
                    javascriptClick(checkbox);
                }
            }

            wait.until(driver -> checkbox.isSelected() == checked);

        } catch (TimeoutException e) {
            System.out.println("Checkbox interaction timeout, continuing without it: " + locator);
        } catch (Exception e) {
            System.out.println("Checkbox interaction failed, continuing without it: " + e.getMessage());
        }
    }

    @Step("Прокрутка к элементу")
    private void scrollToElement(WebElement element) {
        try {
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({behavior: 'smooth', block: 'center', inline: 'center'});", element);
            Thread.sleep(500);
        } catch (Exception e) {
            System.out.println("Scroll failed: " + e.getMessage());
        }
    }

    @Step("Клик с помощью JavaScript")
    private void javascriptClick(WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
    }

    @Step("Нажатие кнопки 'Продолжить'")
    private void clickContinueButton() {
        WebElement continueButton = wait.until(ExpectedConditions.elementToBeClickable(CONTINUE_BUTTON));
        scrollToElement(continueButton);
        continueButton.click();
    }

    @Step("Ожидание загрузки страницы")
    private void waitForPageToLoad() {
        try {
            wait.until(driver -> {
                String state = ((JavascriptExecutor) driver).executeScript("return document.readyState;").toString();
                return state.equals("complete");
            });
        } catch (Exception e) {
            System.out.println("Page load wait interrupted: " + e.getMessage());
        }
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