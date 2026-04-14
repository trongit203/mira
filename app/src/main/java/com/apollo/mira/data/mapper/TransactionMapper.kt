package com.apollo.mira.data.mapper

import com.apollo.mira.data.local.entity.TransactionEntity
import com.apollo.mira.domain.model.DataSource
import com.apollo.mira.domain.model.Transaction
import com.apollo.mira.domain.model.TransactionType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionMapper @Inject constructor() {

//    Domain -> Entity: dung apply vi Entity co var (mutable)
    fun toEntity(domain: Transaction): TransactionEntity =
            TransactionEntity().apply {
                id = domain.id
                amount = domain.amount
                category = domain.category
                note = domain.note.trim()
                type = domain.type.name
                timestamp = domain.timestamp
                source = domain.source.name
            }
//    Entity -> Domain: dung contructor truc tiep vi Transaction la immutable
    // let dung de safe-parse enum - tranh crash neu DB co gia tri la
    fun toDomain(entity: TransactionEntity): Transaction =
            Transaction(
                id = entity.id,
                amount = entity.amount,
                category = entity.category,
                note = entity.note,
                timestamp = entity.timestamp,

                // runCatching + getOrDefault: an toan hon valueOf()
                // valueOf() throw exception neu string khong match enum
                type = runCatching {
                    TransactionType.valueOf(entity.type)
                }.getOrDefault(TransactionType.EXPENSE),

                source = runCatching {
                    DataSource.valueOf(entity.source)
                }.getOrDefault(DataSource.MANUAL)
            )

    fun toDomainList(entities: List<TransactionEntity>): List<Transaction> = entities.map(::toDomain)
}