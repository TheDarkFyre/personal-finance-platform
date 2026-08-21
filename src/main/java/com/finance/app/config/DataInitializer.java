package com.finance.app.config;

import com.finance.app.entity.*;
import com.finance.app.repository.AccountRepository;
import com.finance.app.repository.CategoryRepository;
import com.finance.app.repository.TransactionRepository;
import com.finance.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final com.finance.app.repository.BudgetRepository budgetRepository;
    private final com.finance.app.repository.SubscriptionRepository subscriptionRepository;


    @Override
    @Transactional
    public void run(String... args) {
        if (categoryRepository.count() == 0) {
            log.info("Seeding initial categories, accounts, and demo transactions...");

            // 1. Categories
            Category food = Category.builder().name("Food & Dining").type(CategoryType.EXPENSE).icon("🍔").color("#F87171").description("Restaurants, groceries, and coffee").build();
            Category housing = Category.builder().name("Housing & Rent").type(CategoryType.EXPENSE).icon("🏠").color("#60A5FA").description("Rent, mortgage, and property taxes").build();
            Category utilities = Category.builder().name("Utilities").type(CategoryType.EXPENSE).icon("💡").color("#FBBF24").description("Electricity, water, internet, and gas").build();
            Category transport = Category.builder().name("Transportation").type(CategoryType.EXPENSE).icon("🚗").color("#34D399").description("Gas, public transit, rideshare, and maintenance").build();
            Category entertainment = Category.builder().name("Entertainment").type(CategoryType.EXPENSE).icon("🎬").color("#A78BFA").description("Movies, subscriptions, gaming, and concerts").build();
            Category health = Category.builder().name("Healthcare & Fitness").type(CategoryType.EXPENSE).icon("🏥").color("#F472B6").description("Medical, gym, and pharmacy").build();
            Category salary = Category.builder().name("Salary & Wages").type(CategoryType.INCOME).icon("💼").color("#10B981").description("Primary employment income").build();
            Category investment = Category.builder().name("Investment Returns").type(CategoryType.INCOME).icon("📈").color("#3B82F6").description("Dividends, capital gains, and interest").build();
            Category misc = Category.builder().name("Miscellaneous").type(CategoryType.EXPENSE).icon("📦").color("#9CA3AF").description("Other discretionary expenses").build();

            categoryRepository.saveAll(List.of(food, housing, utilities, transport, entertainment, health, salary, investment, misc));

            // 2. Demo User
            User user = User.builder()
                    .email("alex@example.com")
                    .firstName("Alex")
                    .lastName("Mercer")
                    .build();
            userRepository.save(user);

            // 3. Demo Accounts
            Account checking = Account.builder()
                    .name("Primary Checking")
                    .type(AccountType.CHECKING)
                    .balance(new BigDecimal("4850.00"))
                    .currency("USD")
                    .user(user)
                    .build();

            Account savings = Account.builder()
                    .name("High-Yield Savings")
                    .type(AccountType.SAVINGS)
                    .balance(new BigDecimal("15200.00"))
                    .currency("USD")
                    .user(user)
                    .build();

            Account creditCard = Account.builder()
                    .name("Travel Rewards Card")
                    .type(AccountType.CREDIT_CARD)
                    .balance(new BigDecimal("420.50"))
                    .currency("USD")
                    .user(user)
                    .build();

            accountRepository.saveAll(List.of(checking, savings, creditCard));

            // 4. Demo Transactions
            LocalDate today = LocalDate.now();

            Transaction t1 = Transaction.builder()
                    .account(checking)
                    .category(salary)
                    .amount(new BigDecimal("3500.00"))
                    .transactionDate(today.minusDays(15))
                    .description("Bi-weekly payroll direct deposit")
                    .build();

            Transaction t2 = Transaction.builder()
                    .account(checking)
                    .category(housing)
                    .amount(new BigDecimal("1200.00"))
                    .transactionDate(today.minusDays(14))
                    .description("Monthly Apartment Rent")
                    .build();

            Transaction t3 = Transaction.builder()
                    .account(checking)
                    .category(food)
                    .amount(new BigDecimal("84.50"))
                    .transactionDate(today.minusDays(7))
                    .description("Weekly Grocery Shopping at Whole Foods")
                    .build();

            Transaction t4 = Transaction.builder()
                    .account(checking)
                    .category(utilities)
                    .amount(new BigDecimal("115.00"))
                    .transactionDate(today.minusDays(5))
                    .description("Electric & High-Speed Internet Bill")
                    .build();

            Transaction t5 = Transaction.builder()
                    .account(checking)
                    .category(entertainment)
                    .amount(new BigDecimal("29.99"))
                    .transactionDate(today.minusDays(2))
                    .description("Streaming Services Subscription")
                    .build();

            transactionRepository.saveAll(List.of(t1, t2, t3, t4, t5));

            // 5. Demo Budgets for Current Month
            int curMonth = today.getMonthValue();
            int curYear = today.getYear();

            Budget b1 = Budget.builder().category(food).limitAmount(new BigDecimal("350.00")).month(curMonth).year(curYear).user(user).build();
            Budget b2 = Budget.builder().category(housing).limitAmount(new BigDecimal("1300.00")).month(curMonth).year(curYear).user(user).build();
            Budget b3 = Budget.builder().category(utilities).limitAmount(new BigDecimal("150.00")).month(curMonth).year(curYear).user(user).build();
            Budget b4 = Budget.builder().category(entertainment).limitAmount(new BigDecimal("100.00")).month(curMonth).year(curYear).user(user).build();

            budgetRepository.saveAll(List.of(b1, b2, b3, b4));

            // 6. Demo Subscriptions & Recurring Bills
            Subscription s1 = Subscription.builder()
                    .name("Netflix Premium (4K)")
                    .amount(new BigDecimal("22.99"))
                    .category(entertainment)
                    .account(creditCard)
                    .frequency(BillingFrequency.MONTHLY)
                    .nextDueDate(today.plusDays(4))
                    .status(SubscriptionStatus.ACTIVE)
                    .user(user)
                    .build();

            Subscription s2 = Subscription.builder()
                    .name("Spotify Duo")
                    .amount(new BigDecimal("14.99"))
                    .category(entertainment)
                    .account(checking)
                    .frequency(BillingFrequency.MONTHLY)
                    .nextDueDate(today.plusDays(12))
                    .status(SubscriptionStatus.ACTIVE)
                    .user(user)
                    .build();

            Subscription s3 = Subscription.builder()
                    .name("Equinox Fitness Club")
                    .amount(new BigDecimal("185.00"))
                    .category(health)
                    .account(checking)
                    .frequency(BillingFrequency.MONTHLY)
                    .nextDueDate(today.plusDays(1))
                    .status(SubscriptionStatus.ACTIVE)
                    .user(user)
                    .build();

            Subscription s4 = Subscription.builder()
                    .name("Google Fiber Internet")
                    .amount(new BigDecimal("70.00"))
                    .category(utilities)
                    .account(checking)
                    .frequency(BillingFrequency.MONTHLY)
                    .nextDueDate(today.plusDays(8))
                    .status(SubscriptionStatus.ACTIVE)
                    .user(user)
                    .build();

            subscriptionRepository.saveAll(List.of(s1, s2, s3, s4));

            log.info("Initial data seeding completed successfully.");
        }
    }
}


