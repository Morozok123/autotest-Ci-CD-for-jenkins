package org.ibs.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.qameta.allure.Step;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseSteps {

    private Connection connection;
    private static final String DB_URL = "jdbc:h2:tcp://qualit.applineselenoid.fvds.ru/mem:testdb";
    private static final String USER = "user";
    private static final String PASS = "pass";

    private static final String TEST_FOOD_NAME = "Докторская колбаса";
    private static final String TEST_FOOD_TYPE = "MEAT";
    private static final int TEST_FOOD_EXOTIC = 0;

    private List<FoodItem> initialFoodItems;
    private int newFoodId;
    private List<FoodItem> foodItemsAfterInsert;

    @Given("У меня есть соединение с базой данных")
    @Step("Установка соединения с базой данных")
    public void setupDatabaseConnection() throws SQLException {
        connection = DriverManager.getConnection(DB_URL, USER, PASS);
        connection.setAutoCommit(false);

        // Получить исходное состояние таблицы
        initialFoodItems = getAllFoodItems();
        System.out.println("Исходное состояние таблицы FOOD:");
        printFoodItems(initialFoodItems);
    }

    @When("Я добавляю новый товар в таблицу FOOD")
    @Step("Добавление нового товара")
    public void addNewFoodItem() throws SQLException {
        newFoodId = getNextFoodId();
        insertFoodItem(newFoodId, TEST_FOOD_NAME, TEST_FOOD_TYPE, TEST_FOOD_EXOTIC);

        foodItemsAfterInsert = getAllFoodItems();
        System.out.println("Таблица FOOD после добавления:");
        printFoodItems(foodItemsAfterInsert);
    }

    @Then("Товар должен быть успешно добавлен")
    @Step("Проверка добавления товара")
    public void verifyFoodItemAdded() {
        assertEquals(initialFoodItems.size() + 1, foodItemsAfterInsert.size(),
                "Новый товар должен быть добавлен в таблицу");
    }

    @Then("Я могу найти добавленный товар")
    @Step("Поиск добавленного товара")
    public void findAddedFoodItem() {
        FoodItem newItem = findFoodItemById(foodItemsAfterInsert, newFoodId);
        assertNotNull(newItem, "Добавленный товар должен присутствовать в таблице");
        assertEquals(TEST_FOOD_NAME, newItem.getFoodName(), "Название товара должно совпадать");
        assertEquals(TEST_FOOD_TYPE, newItem.getFoodType(), "Тип товара должно совпадать");
        assertEquals(TEST_FOOD_EXOTIC, newItem.getFoodExotic(), "Флаг экзотичности должен совпадать");
    }

    @When("Я удаляю добавленный товар")
    @Step("Удаление добавленного товара")
    public void deleteAddedFoodItem() throws SQLException {
        deleteFoodItem(newFoodId);
    }

    @Then("Таблица должна вернуться в исходное состояние")
    @Step("Проверка возврата к исходному состоянию")
    public void verifyTableReturnedToInitialState() throws SQLException {
        List<FoodItem> foodItemsAfterDelete = getAllFoodItems();
        System.out.println("Таблица FOOD после удаления:");
        printFoodItems(foodItemsAfterDelete);

        assertEquals(initialFoodItems.size(), foodItemsAfterDelete.size(),
                "Таблица должна вернуться к исходному состоянию после удаления");

        FoodItem deletedItem = findFoodItemById(foodItemsAfterDelete, newFoodId);
        assertNull(deletedItem, "Удаленный товар не должен присутствовать в таблице");

        connection.close();
    }

    // Вспомогательные методы
    @Step("Получение следующего ID для товара")
    private int getNextFoodId() throws SQLException {
        String sql = "SELECT MAX(FOOD_ID) + 1 as NEXT_ID FROM FOOD";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt("NEXT_ID");
            }
        }
        return 1;
    }

    @Step("Получение всех товаров из таблицы FOOD")
    private List<FoodItem> getAllFoodItems() throws SQLException {
        List<FoodItem> foodItems = new ArrayList<>();
        String sql = "SELECT * FROM FOOD ORDER BY FOOD_ID";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                FoodItem item = new FoodItem(
                        rs.getInt("FOOD_ID"),
                        rs.getString("FOOD_NAME"),
                        rs.getString("FOOD_TYPE"),
                        rs.getInt("FOOD_EXOTIC")
                );
                foodItems.add(item);
            }
        }
        return foodItems;
    }

    @Step("Добавление товара")
    private void insertFoodItem(int id, String name, String type, int exotic) throws SQLException {
        String sql = "INSERT INTO FOOD (FOOD_ID, FOOD_NAME, FOOD_TYPE, FOOD_EXOTIC) VALUES (?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.setString(2, name);
            pstmt.setString(3, type);
            pstmt.setInt(4, exotic);
            pstmt.executeUpdate();
        }
    }

    @Step("Удаление товара")
    private void deleteFoodItem(int id) throws SQLException {
        String sql = "DELETE FROM FOOD WHERE FOOD_ID = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    private FoodItem findFoodItemById(List<FoodItem> foodItems, int id) {
        return foodItems.stream()
                .filter(item -> item.getFoodId() == id)
                .findFirst()
                .orElse(null);
    }

    @Step("Вывод списка товаров")
    private void printFoodItems(List<FoodItem> foodItems) {
        for (FoodItem item : foodItems) {
            System.out.println(item);
        }
        System.out.println("---");
    }

    private static class FoodItem {
        private int foodId;
        private String foodName;
        private String foodType;
        private int foodExotic;

        public FoodItem(int foodId, String foodName, String foodType, int foodExotic) {
            this.foodId = foodId;
            this.foodName = foodName;
            this.foodType = foodType;
            this.foodExotic = foodExotic;
        }

        public int getFoodId() { return foodId; }
        public String getFoodName() { return foodName; }
        public String getFoodType() { return foodType; }
        public int getFoodExotic() { return foodExotic; }

        @Override
        public String toString() {
            return String.format("FoodItem{id=%d, name='%s', type='%s', exotic=%d}",
                    foodId, foodName, foodType, foodExotic);
        }
    }
}