/*
 * Copyright (c) 2022.
 * created by S. Mansour
 */

package com.demat.invoice.aws.utils;

import com.amazonaws.AmazonServiceException;

public class UnhandledCharacterException extends AmazonServiceException {
  public UnhandledCharacterException(String errorMessage) {
    super(errorMessage);
  }
}
