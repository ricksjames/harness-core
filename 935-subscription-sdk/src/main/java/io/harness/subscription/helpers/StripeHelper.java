/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.subscription.helpers;

import com.stripe.model.Price;
import io.harness.subscription.dto.*;
import io.harness.subscription.params.CustomerParams;
import io.harness.subscription.params.SubscriptionParams;

import java.util.List;

public interface StripeHelper {
  CustomerDetailDTO createCustomer(CustomerParams customerParams);

  CustomerDetailDTO updateCustomer(CustomerParams customerParams);

  CustomerDetailDTO getCustomer(String customerId);

  Price getPrice(String lookupKey);
  PriceCollectionDTO listPrices(List<String> lookupKeys);
  SubscriptionDetailDTO createSubscription(SubscriptionParams subscriptionParams);
  SubscriptionDetailDTO updateSubscription(SubscriptionParams subscriptionParams);
  SubscriptionDetailDTO updateSubscriptionDefaultPayment(SubscriptionParams subscriptionParams);
  void cancelSubscription(SubscriptionParams subscriptionParams);
  SubscriptionDetailDTO retrieveSubscription(SubscriptionParams subscriptionParams);
  InvoiceDetailDTO previewInvoice(SubscriptionParams subscriptionParams);

  PaymentMethodCollectionDTO listPaymentMethods(String customerId);
}
