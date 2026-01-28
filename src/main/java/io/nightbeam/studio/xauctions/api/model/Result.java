package io.nightbeam.studio.xauctions.api.model;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Represents the result of an operation that can succeed or fail.
 *
 * @param <T> The type of the value returned on success.
 */
public class Result<T> {

    private final T value;
    private final String error;
    private final boolean success;

    private Result(T value, String error, boolean success) {
        this.value = value;
        this.error = error;
        this.success = success;
    }

    public static <T> Result<T> success(T value) {
        return new Result<>(value, null, true);
    }

    public static <T> Result<T> success() {
        return new Result<>(null, null, true);
    }

    public static <T> Result<T> error(String error) {
        return new Result<>(null, error, false);
    }

    public boolean isSuccess() {
        return success;
    }

    public T getValue() {
        if (!success) {
            throw new IllegalStateException("Cannot get value from failed result: " + error);
        }
        return value;
    }

    public String getError() {
        if (success) {
            throw new IllegalStateException("Cannot get error from successful result");
        }
        return error;
    }

    public <U> Result<U> map(Function<T, U> mapper) {
        if (success) {
            return Result.success(mapper.apply(value));
        } else {
            return Result.error(error);
        }
    }

    public <U> Result<U> flatMap(Function<T, Result<U>> mapper) {
        if (success) {
            return mapper.apply(value);
        } else {
            return Result.error(error);
        }
    }

    public void ifSuccess(Consumer<T> consumer) {
        if (success) {
            consumer.accept(value);
        }
    }

    public void ifError(Consumer<String> consumer) {
        if (!success) {
            consumer.accept(error);
        }
    }

    public Optional<T> toOptional() {
        return success ? Optional.ofNullable(value) : Optional.empty();
    }
}
