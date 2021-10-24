/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfinance.p2pcomm.Statistics;

import com.pfinance.p2pcomm.FileHandler.FileHandler;
import com.pfinance.p2pcomm.Session;
import com.pfinance.p2pcomm.Transaction.Penalty;
import com.pfinance.p2pcomm.Transaction.UTXO;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;

/**
 *
 * @author averypozzobon
 */
public class Statistics {
    Session session;
    ArrayList<TransactionInOut> walletInOuts = new ArrayList<>();
    
    public Statistics(Session session) {
        this.session = session;
    }
    
    public ArrayList<TransactionInOut> getWalletInOuts() {
        this.walletInOuts.clear();
        getTransactionsOut();
        getTransactionsIn();
        walletInOuts.sort((a,b) -> Long.compare(b.getDate(), a.getDate()));
        return walletInOuts;
    }
    
    public void getTransactionsOut() {
        File f = new File(session.getPath() + "/wallets/" + session.getWallet().getName() + "/used_utxos/");
        File[] files = f.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isFile();
            }
        });
        if (files == null) return;
        for (int x = 0; x < files.length; x++) {
            FileHandler handler = new FileHandler();
            try {
               UTXO utxo = (UTXO) handler.readObject(files[x].getPath());
               if (utxo != null) {
                    TransactionInOut txn_in = new TransactionInOut(Long.valueOf(utxo.getTimestampIn()),utxo.getPreviousHash(),utxo.toFloat());
                    TransactionInOut txn_out = new TransactionInOut(Long.valueOf(utxo.getTimestampOut()),utxo.getHashOut(),-utxo.toFloat());
                   if (walletInOuts.contains(txn_in)) {
                       walletInOuts.get(walletInOuts.lastIndexOf(txn_in)).addAmount(utxo.toFloat());
                   } else {
                       walletInOuts.add(txn_in);
                   }
                   if (walletInOuts.contains(txn_out)) {
                       walletInOuts.get(walletInOuts.lastIndexOf(txn_out)).addAmount(-utxo.toFloat());
                   } else {
                       walletInOuts.add(txn_out);
                   }
               }
            } catch (Exception e) {}
        }
    }
    
    public void getTransactionsIn() {
        File f = new File(session.getPath() + "/wallets/" + session.getWallet().getName() + "/utxos/");
        File[] files = f.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isFile();
            }
        });
        if (files == null) return;
        for (int x = 0; x < files.length; x++) {
            FileHandler handler = new FileHandler();
            try {
               UTXO utxo = (UTXO) handler.readObject(files[x].getPath());
               if (utxo != null) {
                   TransactionInOut txn_in = new TransactionInOut(Long.valueOf(utxo.getTimestampIn()),utxo.getPreviousHash(),utxo.toFloat());
                   if (walletInOuts.contains(txn_in)) {
                       walletInOuts.get(walletInOuts.lastIndexOf(txn_in)).addAmount(utxo.toFloat());
                   } else {
                       walletInOuts.add(txn_in);
                   }
               }
            } catch (Exception e) {}
        }
    }
}
