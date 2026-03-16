package pl.allegro.tech.common.pullrequestmanager.domain

import java.time.Instant
import java.util.*

interface AvailableSlots {

    fun takeAvailableSlot(workflowId: UUID, concurrencyGroup: String, changeTimestamp: Instant): TakenSlot
    fun releaseSlot(slotId: SlotId)
    fun releaseSlotBy(workflowId: UUID, concurrencyGroup: String, changeTimestamp: Instant)
    fun saveContext(slotId: SlotId, slotContext: SlotContext)
    fun getContext(slotId: SlotId): SlotContext?
}

typealias SlotId = UUID

data class TakenSlot(val id: SlotId, val workflowConcurrencyGroup: Int, val changeTimestamp: Instant)
data class SlotContext(val slotId: SlotId, val repository: Repository, val checkRunId: String, val ref: String, val checkTitle: String, val pullRequestNumber: Long)

class NoSlotsAvailable(workflowId: UUID) : RuntimeException("No free slots available for $workflowId")
