package com.showmethemoney.transaction.application;

import com.showmethemoney.category.application.CategoryService;
import com.showmethemoney.common.BusinessException;
import com.showmethemoney.common.ErrorCode;
import com.showmethemoney.transaction.domain.Transaction;
import com.showmethemoney.transaction.infrastructure.TransactionMapper;
import com.showmethemoney.transaction.interfaces.dto.CreateTransactionRequest;
import com.showmethemoney.transaction.interfaces.dto.TransactionPageResponse;
import com.showmethemoney.transaction.interfaces.dto.TransactionResponse;
import com.showmethemoney.transaction.interfaces.dto.UpdateTransactionRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionMapper transactionMapper;
    private final CategoryService categoryService;

    public TransactionService(TransactionMapper transactionMapper, CategoryService categoryService) {
        this.transactionMapper = transactionMapper;
        this.categoryService = categoryService;
    }

    @Transactional
    public void create(Long userId, CreateTransactionRequest request) {
        Long uuidCategory = categoryService.validateAndGetId(request.categoryCode(), request.type());
        Transaction tx = new Transaction();
        tx.setUuidUser(userId);
        tx.setUuidCategory(uuidCategory);
        tx.setType(request.type());
        tx.setAmount(request.amount());
        tx.setMemo(request.memo());
        tx.setTransactionDate(request.transactionAt());
        transactionMapper.insert(tx);
    }

    @Transactional(readOnly = true)
    public TransactionPageResponse getList(Long userId, TransactionListRequest request) {
        int offset = request.page() * request.size();
        List<TransactionResponse> content = transactionMapper.findAll(userId, request, offset).stream()
                .map(this::toResponse)
                .toList();
        long totalElements = transactionMapper.countAll(userId, request);
        int totalPages = (int) Math.ceil((double) totalElements / request.size());
        BigDecimal totalIncome = transactionMapper.sumAmountByType(userId, request, 1);
        BigDecimal totalExpense = transactionMapper.sumAmountByType(userId, request, 0);
        return new TransactionPageResponse(content, totalElements, totalPages, totalIncome, totalExpense);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getOne(Long userId, Long id) {
        Transaction tx = findOwnedTransaction(userId, id);
        return toResponse(tx);
    }

    @Transactional
    public void update(Long userId, Long id, UpdateTransactionRequest request) {
        Transaction existing = findOwnedTransaction(userId, id);

        Long uuidCategory = null;
        if (request.categoryCode() != null || request.type() != null) {
            String newCode = request.categoryCode() != null ? request.categoryCode() : existing.getCategoryCode();
            Integer newType = request.type() != null ? request.type() : existing.getType();
            uuidCategory = categoryService.validateAndGetId(newCode, newType);
        }

        Transaction update = new Transaction();
        update.setUuid(id);
        update.setType(request.type());
        update.setUuidCategory(uuidCategory);
        update.setAmount(request.amount());
        update.setMemo(request.memo());
        update.setTransactionDate(request.transactionAt());
        transactionMapper.update(update);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        findOwnedTransaction(userId, id);
        transactionMapper.softDelete(id);
    }

    private Transaction findOwnedTransaction(Long userId, Long id) {
        Transaction tx = transactionMapper.findById(id);
        if (tx == null || tx.getDeletedAt() != null) throw new BusinessException(ErrorCode.TRANSACTION_NOT_FOUND);
        if (!tx.getUuidUser().equals(userId)) throw new BusinessException(ErrorCode.FORBIDDEN);
        return tx;
    }

    private TransactionResponse toResponse(Transaction tx) {
        String typeStr = tx.getType() == 1 ? "INCOME" : "EXPENSE";
        return new TransactionResponse(tx.getUuid(), typeStr, tx.getCategoryCode(),
                tx.getCategoryName(), tx.getAmount(), tx.getMemo(), tx.getTransactionDate());
    }
}
