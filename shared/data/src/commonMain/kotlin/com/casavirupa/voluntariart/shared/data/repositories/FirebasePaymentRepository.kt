package com.casavirupa.voluntariart.shared.data.repositories

import com.casavirupa.voluntariart.shared.data.repositories.requests.FirebasePayment
import com.casavirupa.voluntariat.shared.domain.PaymentRepository
import com.casavirupa.voluntariat.shared.model.payment.Payment
import com.casavirupa.voluntariat.shared.model.payment.PaymentId
import com.casavirupa.voluntariat.shared.model.user.UserId
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.YearMonth
import kotlinx.datetime.number

class FirebasePaymentRepository(
    private val firestore: FirebaseFirestore,
) : PaymentRepository {
    override fun getPaymentByYearMonth(yearMonth: YearMonth): Flow<Payment?> =
        firestore
            .collection("payments")
            .where { ("year" equalTo yearMonth.year) and ("month" equalTo yearMonth.month.number) }
            .snapshots
            .map { snapshot ->
                snapshot
                    .documents
                    .map { doc -> doc.data<FirebasePayment>().toDomainModel(doc.id) }
                    .firstOrNull()
            }

    override suspend fun pay(
        id: PaymentId,
        yearMonth: YearMonth
    ): Result<Unit> = runCatching {
        firestore
            .collection("payments")
            .document(id.value)
            .updateFields {
                "paid" to true
            }
    }

    override suspend fun addPaymentIfNotExist(
        userId: UserId,
        yearMonth: YearMonth,
        amount: Double,
    ): Result<Unit> = runCatching {
        val existingPayments = firestore
            .collection("payments")
            .where {
                ("userId" equalTo userId.value) and
                ("year" equalTo yearMonth.year) and
                ("month" equalTo yearMonth.month.number)
            }
            .get()

        if (existingPayments.documents.isEmpty()) {
            firestore
                .collection("payments")
                .add(
                    FirebasePayment(
                        userId = userId.value,
                        year = yearMonth.year,
                        month = yearMonth.month.number,
                        paid = false,
                        amount = amount,
                    )
                )
        }
    }
}

private fun FirebasePayment.toDomainModel(id: String) =
    Payment(
        id = PaymentId(id),
        yearMonth = YearMonth(year, month),
        paid = paid,
        amount = amount,
    )