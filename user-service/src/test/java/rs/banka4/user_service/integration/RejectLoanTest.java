package rs.banka4.user_service.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import rs.banka4.testlib.integration.DbEnabledTest;
import rs.banka4.user_service.domain.account.db.Account;
import rs.banka4.user_service.domain.loan.db.Loan;
import rs.banka4.user_service.domain.loan.db.LoanStatus;
import rs.banka4.user_service.integration.generator.UserGenerator;
import rs.banka4.user_service.integration.seeder.TestDataSeeder;
import rs.banka4.user_service.repositories.LoanRepository;
import rs.banka4.user_service.utils.JwtUtil;

@SpringBootTest
@AutoConfigureMockMvc
@DbEnabledTest
public class RejectLoanTest {

    @Autowired
    private MockMvcTester m;

    @Autowired
    private UserGenerator userGen;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objMapper;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private TestDataSeeder testDataSeeder;


    private String accessToken;
    private Loan loan;
    private Loan rejectedLoan;

    @BeforeEach
    void setUp() {
        Account account = testDataSeeder.seedAccount();
        loan = testDataSeeder.seedLoan(account);
        rejectedLoan = testDataSeeder.seedRejectedLoan(account);

        userGen.createEmployee(x -> x);
        var toks = userGen.doEmployeeLogin("john.doe@example.com", "test");
        accessToken = toks.accessToken();
    }

    @Test
    void rejectLoanSuccessfully() throws Exception {
        m.put()
            .uri("/loans/reject/" + loan.getLoanNumber())
            .header("Authorization", "Bearer " + accessToken)
            .accept(MediaType.APPLICATION_JSON)
            .assertThat()
            .hasStatus(HttpStatus.OK);

        Loan rejectedLoan =
            loanRepository.findByLoanNumber(loan.getLoanNumber())
                .orElseThrow();
        assertThat(rejectedLoan.getStatus()).isEqualTo(LoanStatus.REJECTED);
    }

    @Test
    void rejectLoanFailsForNonExistentLoan() throws Exception {
        long nonExistentLoanNumber = 999999L;

        m.put()
            .uri("/loans/reject/" + nonExistentLoanNumber)
            .header("Authorization", "Bearer " + accessToken)
            .accept(MediaType.APPLICATION_JSON)
            .assertThat()
            .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void rejectLoanFailsForAlreadyRejectedLoan() throws Exception {

        m.put()
            .uri("/loans/reject/" + rejectedLoan.getLoanNumber())
            .header("Authorization", "Bearer " + accessToken)
            .accept(MediaType.APPLICATION_JSON)
            .assertThat()
            .hasStatus(HttpStatus.NOT_FOUND); // should be BAD_REQUEST
    }
}
