package com.clopez021.mine_arena.integration.meshy;

public class MeshyExceptions {
  public static class InvalidApiKeyException extends RuntimeException {
    public InvalidApiKeyException() {
      super("Invalid Meshy API key. Provide a valid API key in the mod's config file.");
    }
  }

  public static class PaymentRequiredException extends RuntimeException {
    public PaymentRequiredException() {
      super(
          "Payment required. Please add funds to the Meshy account associated with your API key.");
    }
  }

  public static class TooManyRequestsException extends RuntimeException {
    public TooManyRequestsException() {
      super("Too many requests. Please wait and try again later.");
    }
  }

  public static class ServerErrorException extends RuntimeException {
    public ServerErrorException() {
      super("A Meshy server error occurred. Please try again later.");
    }
  }
}
