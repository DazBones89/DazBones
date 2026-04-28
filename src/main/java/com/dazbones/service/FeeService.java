package com.dazbones.service;

import com.dazbones.model.Fee;
import com.dazbones.model.Player;
import com.dazbones.repository.FeeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FeeService {

    private final FeeRepository feeRepository;
    private final PlayerService playerService;

    public FeeService(FeeRepository feeRepository,
                      PlayerService playerService) {
        this.feeRepository = feeRepository;
        this.playerService = playerService;
    }

    public List<Fee> findAll() {
        return feeRepository.findAll();
    }

    public Fee findByPlayerId(Long playerId) {
        return feeRepository.findByPlayerId(playerId).orElse(null);
    }

    public Fee findById(Long id) {
        return feeRepository.findById(id).orElse(null);
    }

    public void save(Fee fee) {
        if (fee.getPaidFlg() == null) {
            fee.setPaidFlg(0);
        }

        if (fee.getAmount() == null) {
            fee.setAmount(0);
        }

        feeRepository.save(fee);
    }

    public void saveOrUpdateByPlayer(Long playerId, Integer paidFlg, Integer amount, String comment) {
        Fee fee = feeRepository.findByPlayerId(playerId).orElse(new Fee());

        fee.setPlayerId(playerId);
        fee.setPaidFlg(paidFlg == null ? 0 : paidFlg);
        fee.setAmount(amount == null ? 0 : amount);
        fee.setComment(comment);

        feeRepository.save(fee);
    }

    public void createMissingFeeRowsForActivePlayers() {
        List<Player> players = playerService.getActivePlayers();

        for (Player player : players) {
            Fee existing = findByPlayerId(player.getId());

            if (existing == null) {
                Fee fee = new Fee();
                fee.setPlayerId(player.getId());
                fee.setPaidFlg(0);
                fee.setAmount(0);
                fee.setComment("");
                feeRepository.save(fee);
            }
        }
    }

    public long countUnpaid() {
        return feeRepository.findByPaidFlgOrderByUpdatedAtDesc(0).size();
    }
}