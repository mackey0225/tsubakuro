package com.nautilus_technologies.tsubakuro.low.auth;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import com.nautilus_technologies.tsubakuro.exception.CoreServiceException;

/**
 * An interface to provide authentication {@link Ticket}.
 */
public interface TicketProvider {

    /**
     * Restores {@link Ticket} from its {@link Ticket#getToken() token text}.
     * <p>
     * The returned ticket may not have access permissions.
     * If {@link Ticket#getAccessExpirationTime()} returns empty, please invoke
     * {@link #refresh(Ticket, long, TimeUnit) refresh()} consequently to the returned one to gain access permissions.
     * </p>
     * @param text the token text
     * @return the restored authentication token
     * @throws IllegalArgumentException if the token text is ill-formed
     */
    Ticket restore(@Nonnull String text);

    /**
     * Obtains a new {@link Ticket} using a pair of user ID and its password.
     * <p>
     * The returned ticket may not have access permissions.
     * If {@link Ticket#getAccessExpirationTime()} returns empty, please invoke
     * {@link #refresh(Ticket, long, TimeUnit) refresh()} consequently to the returned one to gain access permissions.
     * </p>
     * @param userId the user ID
     * @param password the password
     * @return the authentication token
     * @throws IOException if I/O error was occurred while requesting authentication to the service
     * @throws CoreServiceException if authentication was failed
     */
    Ticket issue(@Nonnull String userId, @Nonnull String password) throws IOException, CoreServiceException;

    /**
     * Requests to extend access expiration time of the given {@link Ticket}.
     * <p>
     * This operation requires which the {@link Ticket#getRefreshExpirationTime() refresh expiration time} is
     * remained in the given ticket.
     * </p>
     * <p>
     * This never modifies the input {@link Ticket} object.
     * </p>
     * @param ticket the old {@link Ticket} to refresh access expiration time
     * @param expiration the maximum time to extend the access expiration from now
     * @param unit the time unit of expiration
     * @return the refreshed {@link Ticket}
     * @throws IllegalArgumentException if the input ticket is not provided by this object
     * @throws IllegalArgumentException if the input ticket does not support
     *      {@link Ticket#getRefreshExpirationTime() refresh}
     * @throws IOException if I/O error was occurred while requesting authentication to the service
     * @throws CoreServiceException if refresh operation was failed in the authentication mechanism
     */
    Ticket refresh(
            @Nonnull Ticket ticket,
            long expiration,
            @Nonnull TimeUnit unit) throws IOException, CoreServiceException;
}
