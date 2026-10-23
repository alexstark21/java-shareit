package ru.practicum.shareit.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findAllByBookerIdOrderByStartDesc(Long bookerId);

    List<Booking> findAllByItemOwnerIdOrderByStartDesc(Long ownerId);

    List<Booking> findAllByItemIdAndStatus(Long itemId, BookingStatus status);

    List<Booking> findAllByItemIdInAndStatus(List<Long> itemIds, BookingStatus status);

    boolean existsByBookerIdAndItemIdAndStatusAndEndBefore(
            Long bookerId, Long itemId, BookingStatus status, LocalDateTime now);

    @Query("select b from Booking b " +
            "where b.id = :bookingId " +
            "and (b.booker.id = :userId or b.item.owner.id = :userId)")
    Optional<Booking> findByIdAndBookerIdOrItemOwnerId(@Param("bookingId") Long bookingId,
                                                       @Param("userId") Long userId);

    @Query("select count(b) > 0 from Booking b " +
            "where b.item.id = :itemId " +
            "and b.status = 'APPROVED' " +
            "and b.start < :end " +
            "and b.end > :start")
    boolean hasOverlap(@Param("itemId") Long itemId, @Param("start") LocalDateTime start,
                       @Param("end") LocalDateTime end);
}
