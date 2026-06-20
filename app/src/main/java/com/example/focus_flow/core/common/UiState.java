package com.example.focus_flow.core.common;

public class UiState<T> {
    public enum Status {
        LOADING, SUCCESS, EMPTY, ERROR
    }

    public final Status status;
    public final T data;
    public final String message;

    private UiState(Status status, T data, String message) {
        this.status = status;
        this.data = data;
        this.message = message;
    }

    public static <T> UiState<T> loading() {
        return new UiState<>(Status.LOADING, null, "");
    }

    public static <T> UiState<T> success(T data) {
        return new UiState<>(Status.SUCCESS, data, "");
    }

    public static <T> UiState<T> empty(String message) {
        return new UiState<>(Status.EMPTY, null, message);
    }

    public static <T> UiState<T> error(String message) {
        return new UiState<>(Status.ERROR, null, message);
    }
}
