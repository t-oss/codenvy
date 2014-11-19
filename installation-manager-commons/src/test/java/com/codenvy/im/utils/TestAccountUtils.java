/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2014] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.im.utils;

import com.codenvy.api.account.shared.dto.AccountReference;
import com.codenvy.im.exceptions.AuthenticationException;
import com.codenvy.im.user.UserCredentials;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import static com.codenvy.im.utils.AccountUtils.SUBSCRIPTION_DATE_FORMAT;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Dmytro Nochevnov
 */
public class TestAccountUtils {

    public static final String SUBSCRIPTION_ID = "subscription_id1";
    private HttpTransport mockTransport;
    private final static String VALID_SUBSCRIPTION = "OnPremises";
    private UserCredentials testCredentials;
    private SimpleDateFormat subscriptionDateFormat;

    @BeforeMethod
    public void setup() {
        mockTransport = mock(HttpTransport.class);
        testCredentials = new UserCredentials("auth token", "accountId");
        subscriptionDateFormat = new SimpleDateFormat(SUBSCRIPTION_DATE_FORMAT);
    }

    @Test
    public void testValidSubscriptionByAccountReference() throws IOException {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        String startDate = subscriptionDateFormat.format(cal.getTime());

        cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 1);
        String endDate = subscriptionDateFormat.format(cal.getTime());

        when(mockTransport.doGet("/account", testCredentials.getToken())).thenReturn("[{"
                                                                                            + "roles:[\"" + AccountUtils.ACCOUNT_OWNER_ROLE + "\"],"
                                                                                            + "accountReference:{id:\"another-id\"}"
                                                                                            + "},{"
                                                                                            + "roles:[\"" + AccountUtils.ACCOUNT_OWNER_ROLE + "\"],"
                                                                                            + "accountReference:{id:\"" +
                                                                                            testCredentials.getAccountId() + "\"}"
                                                                                            + "}]");
        when(mockTransport.doGet("/account/" + testCredentials.getAccountId() + "/subscriptions", testCredentials.getToken()))
                .thenReturn("[{serviceId:" + VALID_SUBSCRIPTION + ",id:" + SUBSCRIPTION_ID + "}]");
        when(mockTransport.doGet("/account/subscriptions/" + SUBSCRIPTION_ID + "/attributes", testCredentials.getToken()))
                .thenReturn("{startDate:\"" + startDate + "\",endDate:\"" + endDate + "\"}");
        
        assertTrue(AccountUtils.isValidSubscription(mockTransport, "", VALID_SUBSCRIPTION, testCredentials));
    }

    @Test
    public void testGetAccountIdWhereUserIsOwner() throws IOException {
        when(mockTransport.doGet("/account", testCredentials.getToken())).thenReturn("[{"
                                                                                     + "roles:[\"account/member\"],"
                                                                                     + "accountReference:{id:\"member-id\"}"
                                                                                     + "},{"
                                                                                     + "roles:[\"" + AccountUtils.ACCOUNT_OWNER_ROLE + "\"],"
                                                                                     + "accountReference:{id:\"id1\",name:\"name1\"}"
                                                                                     + "},{"
                                                                                     + "roles:[\"" + AccountUtils.ACCOUNT_OWNER_ROLE + "\"],"
                                                                                     + "accountReference:{id:\"id2\",name:\"name2\"}"
                                                                                     + "}]");
        AccountReference accountReference = AccountUtils.getAccountReferenceWhereUserIsOwner(mockTransport, "", testCredentials.getToken(), null);
        assertNotNull(accountReference);
        assertEquals(accountReference.getId(), "id1");
        assertEquals(accountReference.getName(), "name1");
    }

    @Test
    public void testGetAccountIdWhereUserIsOwnerReturnNull() throws IOException {
        when(mockTransport.doGet("/account", testCredentials.getToken())).thenReturn("[{"
                                                                                     + "roles:[\"account/member\"],"
                                                                                     + "accountReference:{id:\"member-id\"}"
                                                                                     + "},{"
                                                                                     + "roles:[\"account/member \"],"
                                                                                     + "accountReference:{id:\"id1\",name:\"name1\"}"
                                                                                     + "},{"
                                                                                     + "roles:[\"account/member\"],"
                                                                                     + "accountReference:{id:\"id2\",name:\"name2\"}"
                                                                                     + "}]");
        AccountReference accountReference = AccountUtils.getAccountReferenceWhereUserIsOwner(mockTransport, "", testCredentials.getToken(), null);
        assertNull(accountReference);
    }

    @Test
    public void testGetAccountIdWhereUserIsOwnerSpecificAccountName() throws IOException {
        when(mockTransport.doGet("/account", testCredentials.getToken())).thenReturn("[{"
                                                                                     + "roles:[\"account/member\"],"
                                                                                     + "accountReference:{id:\"member-id\"}"
                                                                                     + "},{"
                                                                                     + "roles:[\"" + AccountUtils.ACCOUNT_OWNER_ROLE + "\"],"
                                                                                     + "accountReference:{id:\"id1\",name:\"name1\"}"
                                                                                     + "},{"
                                                                                     + "roles:[\"" + AccountUtils.ACCOUNT_OWNER_ROLE + "\"],"
                                                                                     + "accountReference:{id:\"id2\",name:\"name2\"}"
                                                                                     + "}]");
        AccountReference accountReference = AccountUtils.getAccountReferenceWhereUserIsOwner(mockTransport, "", testCredentials.getToken(), "name2");
        assertNotNull(accountReference);
        assertEquals(accountReference.getId(), "id2");
        assertEquals(accountReference.getName(), "name2");
    }

    @Test
    public void testGetAccountIdWhereUserIsOwnerSpecificAccountNameReturnNullIfAccountWasNotFound() throws IOException {
        when(mockTransport.doGet("/account", testCredentials.getToken())).thenReturn("[{"
                                                                                     + "roles:[\"account/member\"],"
                                                                                     + "accountReference:{id:\"member-id\"}"
                                                                                     + "},{"
                                                                                     + "roles:[\"" + AccountUtils.ACCOUNT_OWNER_ROLE + "\"],"
                                                                                     + "accountReference:{id:\"id1\",name:\"name1\"}"
                                                                                     + "},{"
                                                                                     + "roles:[\"" + AccountUtils.ACCOUNT_OWNER_ROLE + "\"],"
                                                                                     + "accountReference:{id:\"id2\",name:\"name2\"}"
                                                                                     + "}]");
        AccountReference accountReference = AccountUtils.getAccountReferenceWhereUserIsOwner(mockTransport, "", testCredentials.getToken(), "name3");
        assertNull(accountReference);
    }

    @Test
    public void testValidSubscriptionByLink() throws IOException {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        String startDate = subscriptionDateFormat.format(cal.getTime());

        cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 1);
        String endDate = subscriptionDateFormat.format(cal.getTime());

        when(mockTransport.doGet("/account", testCredentials.getToken())).thenReturn("[{"
                                                                + "links:[{\"rel\":\"subscriptions\",\"href\":\"/account/"
                                                                + testCredentials.getAccountId()
                                                                + "/subscriptions\"}],"
                                                                + "roles:[\"" + AccountUtils.ACCOUNT_OWNER_ROLE + "\"]"
                                                                + "}]");

        when(mockTransport.doGet("/account/" + testCredentials.getAccountId() + "/subscriptions", testCredentials.getToken()))
                .thenReturn("[{serviceId:" + VALID_SUBSCRIPTION + ",id:" + SUBSCRIPTION_ID + "}]");
        when(mockTransport.doGet("/account/subscriptions/" + SUBSCRIPTION_ID + "/attributes", testCredentials.getToken()))
                .thenReturn("{startDate:\"" + startDate + "\",endDate:\"" + endDate + "\"}");
        
        assertTrue(AccountUtils.isValidSubscription(mockTransport, "", VALID_SUBSCRIPTION, testCredentials));
    }

    @Test
    public void testInvalidSubscription() throws IOException {
        when(mockTransport.doGet("/account", testCredentials.getToken())).thenReturn("[{"
                                                                             + "roles:[\"" + AccountUtils.ACCOUNT_OWNER_ROLE + "\"],"
                                                                             + "accountReference:{id:\"" + testCredentials.getAccountId() + "\"}"
                                                                             + "}]");
        when(mockTransport.doGet("/account/" + testCredentials.getAccountId() + "/subscriptions", testCredentials.getToken()))
        .thenReturn("[{serviceId:invalid}]");
        
        assertFalse(AccountUtils.isValidSubscription(mockTransport, "", VALID_SUBSCRIPTION, testCredentials));        
    }

    @Test(expectedExceptions = AuthenticationException.class,
          expectedExceptionsMessageRegExp = "Authentication error. Authentication token might be expired or invalid.")
    public void testInvalidAuthentication() throws IOException {
        doThrow(new HttpException(403, "auth error"))
            .when(mockTransport)
            .doGet("/account/" + testCredentials.getAccountId() + "/subscriptions", testCredentials.getToken());

        AccountUtils.isValidSubscription(mockTransport, "", VALID_SUBSCRIPTION, testCredentials);
    }

    @Test
    public void testValidSubscriptionByDate() throws IOException {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        String startDate = subscriptionDateFormat.format(cal.getTime());

        cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 1);
        String endDate = subscriptionDateFormat.format(cal.getTime());

        when(mockTransport.doGet("/account/" + testCredentials.getAccountId() + "/subscriptions", testCredentials.getToken()))
                .thenReturn("[{serviceId:" + VALID_SUBSCRIPTION + ",id:" + SUBSCRIPTION_ID + "}]");
        when(mockTransport.doGet("/account/subscriptions/" + SUBSCRIPTION_ID + "/attributes", testCredentials.getToken()))
                .thenReturn("{startDate:\"" + startDate + "\",endDate:\"" + endDate + "\"}");

        assertTrue(AccountUtils.isValidSubscription(mockTransport, "", VALID_SUBSCRIPTION, testCredentials));
    }

    @Test
    public void testOutdatedSubscription() throws IOException {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -2);
        String startDate = subscriptionDateFormat.format(cal.getTime());

        cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        String endDate = subscriptionDateFormat.format(cal.getTime());

        when(mockTransport.doGet("/account/" + testCredentials.getAccountId() + "/subscriptions", testCredentials.getToken()))
                .thenReturn("[{serviceId:" + VALID_SUBSCRIPTION + ",id:" + SUBSCRIPTION_ID + "}]");
        when(mockTransport.doGet("/account/subscriptions/" + SUBSCRIPTION_ID + "/attributes", testCredentials.getToken()))
                .thenReturn("{startDate:\"" + startDate + "\",endDate:\"" + endDate + "\"}");

        assertFalse(AccountUtils.isValidSubscription(mockTransport, "", VALID_SUBSCRIPTION, testCredentials));
    }

    @Test
    public void testInvalidSubscriptionStartDateIsTomorrow() throws IOException {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 1);
        String startDate = subscriptionDateFormat.format(cal.getTime());

        cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 2);
        String endDate = subscriptionDateFormat.format(cal.getTime());

        when(mockTransport.doGet("/account/" + testCredentials.getAccountId() + "/subscriptions", testCredentials.getToken()))
                .thenReturn("[{serviceId:" + VALID_SUBSCRIPTION + ",id:" + SUBSCRIPTION_ID + "}]");
        when(mockTransport.doGet("/account/subscriptions/" + SUBSCRIPTION_ID + "/attributes", testCredentials.getToken()))
                .thenReturn("{startDate:\"" + startDate + "\",endDate:\"" + endDate + "\"}");

        assertFalse(AccountUtils.isValidSubscription(mockTransport, "", VALID_SUBSCRIPTION, testCredentials));
    }

    @Test(expectedExceptions = IllegalStateException.class,
          expectedExceptionsMessageRegExp = "Can't validate subscription. Start date attribute is absent")
    public void testValidSubscriptionByDateStartDateIsAbsent() throws IOException {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 1);
        String endDate = subscriptionDateFormat.format(cal.getTime());

        when(mockTransport.doGet("/account/" + testCredentials.getAccountId() + "/subscriptions", testCredentials.getToken()))
                .thenReturn("[{serviceId:" + VALID_SUBSCRIPTION + ",id:" + SUBSCRIPTION_ID + "}]");
        when(mockTransport.doGet("/account/subscriptions/" + SUBSCRIPTION_ID + "/attributes", testCredentials.getToken()))
                .thenReturn("{endDate:\"" + endDate + "\"}");

        AccountUtils.isValidSubscription(mockTransport, "", VALID_SUBSCRIPTION, testCredentials);
    }

    @Test(expectedExceptions = IllegalStateException.class,
          expectedExceptionsMessageRegExp = "Can't validate subscription. End date attribute is absent")
    public void testValidSubscriptionByDateEndDateIsAbsent() throws IOException {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        String startDate = subscriptionDateFormat.format(cal.getTime());

        when(mockTransport.doGet("/account/" + testCredentials.getAccountId() + "/subscriptions", testCredentials.getToken()))
                .thenReturn("[{serviceId:" + VALID_SUBSCRIPTION + ",id:" + SUBSCRIPTION_ID + "}]");
        when(mockTransport.doGet("/account/subscriptions/" + SUBSCRIPTION_ID + "/attributes", testCredentials.getToken()))
                .thenReturn("{startDate:\"" + startDate + "\"}");

        AccountUtils.isValidSubscription(mockTransport, "", VALID_SUBSCRIPTION, testCredentials);
    }

    @Test(expectedExceptions = IllegalStateException.class,
          expectedExceptionsMessageRegExp = "Can't validate subscription. Start date attribute has wrong format: .*")
    public void testInvalidSubscriptionStartDateIsWrong() throws IOException {
        SimpleDateFormat subscriptionDateWrongFormat = new SimpleDateFormat("yyyy.MM.dd");

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 1);
        String startDate = subscriptionDateWrongFormat.format(cal.getTime());

        cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 2);
        String endDate = subscriptionDateFormat.format(cal.getTime());

        when(mockTransport.doGet("/account/" + testCredentials.getAccountId() + "/subscriptions", testCredentials.getToken()))
                .thenReturn("[{serviceId:" + VALID_SUBSCRIPTION + ",id:" + SUBSCRIPTION_ID + "}]");
        when(mockTransport.doGet("/account/subscriptions/" + SUBSCRIPTION_ID + "/attributes", testCredentials.getToken()))
                .thenReturn("{startDate:\"" + startDate + "\",endDate:\"" + endDate + "\"}");

        AccountUtils.isValidSubscription(mockTransport, "", VALID_SUBSCRIPTION, testCredentials);
    }

    @Test(expectedExceptions = IllegalStateException.class,
          expectedExceptionsMessageRegExp = "Can't validate subscription. End date attribute has wrong format: .*")
    public void testInvalidSubscriptionEndDateIsWrong() throws IOException {
        SimpleDateFormat subscriptionDateWrongFormat = new SimpleDateFormat("yyyy.MM.dd");

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 1);
        String startDate = subscriptionDateFormat.format(cal.getTime());

        cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 2);
        String endDate = subscriptionDateWrongFormat.format(cal.getTime());

        when(mockTransport.doGet("/account/" + testCredentials.getAccountId() + "/subscriptions", testCredentials.getToken()))
                .thenReturn("[{serviceId:" + VALID_SUBSCRIPTION + ",id:" + SUBSCRIPTION_ID + "}]");
        when(mockTransport.doGet("/account/subscriptions/" + SUBSCRIPTION_ID + "/attributes", testCredentials.getToken()))
                .thenReturn("{startDate:\"" + startDate + "\",endDate:\"" + endDate + "\"}");

        AccountUtils.isValidSubscription(mockTransport, "", VALID_SUBSCRIPTION, testCredentials);
    }
}