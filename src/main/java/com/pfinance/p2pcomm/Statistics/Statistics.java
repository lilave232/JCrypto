/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfinance.p2pcomm.Statistics;

import com.pfinance.p2pcomm.FileHandler.FileHandler;
import com.pfinance.p2pcomm.Session;
import com.pfinance.p2pcomm.Transaction.Penalty;
import com.pfinance.p2pcomm.Transaction.Transaction;
import com.pfinance.p2pcomm.Transaction.TransactionInput;
import com.pfinance.p2pcomm.Transaction.TransactionOutput;
import com.pfinance.p2pcomm.Transaction.UTXO;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
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
    
    public ArrayList<TransactionInOut> getWalletInOuts(String address) {
        ArrayList<TransactionInOut> localInOuts = new ArrayList<>();
        getTransactionsOut(address,localInOuts);
        getTransactionsIn(address,localInOuts);
        getTransactionsPendingOut(address,localInOuts);
        getTransactionsPendingIn(address,localInOuts);
        localInOuts.sort((a,b) -> Long.compare(b.getDate(), a.getDate()));
        return localInOuts;
    }
    
    public ArrayList<TransactionInOut> getWalletInOuts() {
        this.walletInOuts.clear();
        getTransactionsOut();
        getTransactionsIn();
        walletInOuts.sort((a,b) -> Long.compare(b.getDate(), a.getDate()));
        return walletInOuts;
    }
    
    public ArrayList<TransactionInOut> getTransactionsOut(String address, ArrayList<TransactionInOut> localInOuts) {
        File f = new File(session.getPath() + "/used_utxos/");
        File[] files = f.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isFile();
            }
        });
        if (files == null) return new ArrayList<>();
        for (int x = 0; x < files.length; x++) {
            FileHandler handler = new FileHandler();
            try {
               UTXO utxo = (UTXO) handler.readObject(files[x].getPath());
               if (utxo != null) {
                    if (!utxo.getAddress().equals(address)) continue;
                    TransactionInOut txn_in = new TransactionInOut(Long.valueOf(utxo.getTimestampIn()),utxo.getPreviousHash(),utxo.getIndex(),utxo.toFloat());
                    TransactionInOut txn_out = new TransactionInOut(Long.valueOf(utxo.getTimestampOut()),utxo.getHashOut(),utxo.getIndex(),utxo.toFloat().negate());
                    if (localInOuts.contains(txn_in)) {
                        localInOuts.get(localInOuts.lastIndexOf(txn_in)).addAmount(utxo.toFloat());
                    } else {
                        localInOuts.add(txn_in);
                    }
                    if (localInOuts.contains(txn_out)) {
                        localInOuts.get(localInOuts.lastIndexOf(txn_out)).addAmount(utxo.toFloat().negate());
                    } else {
                        localInOuts.add(txn_out);
                    }
               }
            } catch (Exception e) {}
        }
        return localInOuts;
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
                    TransactionInOut txn_in = new TransactionInOut(Long.valueOf(utxo.getTimestampIn()),utxo.getPreviousHash(),utxo.getIndex(),utxo.toFloat());
                    TransactionInOut txn_out = new TransactionInOut(Long.valueOf(utxo.getTimestampOut()),utxo.getHashOut(),utxo.getIndex(),utxo.toFloat().negate());
                   if (walletInOuts.contains(txn_in)) {
                       walletInOuts.get(walletInOuts.lastIndexOf(txn_in)).addAmount(utxo.toFloat());
                   } else {
                       walletInOuts.add(txn_in);
                   }
                   if (walletInOuts.contains(txn_out)) {
                       walletInOuts.get(walletInOuts.lastIndexOf(txn_out)).addAmount(utxo.toFloat().negate());
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
                   TransactionInOut txn_in = new TransactionInOut(Long.valueOf(utxo.getTimestampIn()),utxo.getPreviousHash(),utxo.getIndex(),utxo.toFloat());
                   if (walletInOuts.contains(txn_in)) {
                       walletInOuts.get(walletInOuts.lastIndexOf(txn_in)).addAmount(utxo.toFloat());
                   } else {
                       walletInOuts.add(txn_in);
                   }
               }
            } catch (Exception e) {}
        }
    }
        
    public ArrayList<TransactionInOut> getTransactionsIn(String address, ArrayList<TransactionInOut> localInOuts) {
        File f = new File(session.getPath() + "/utxos/");
        File[] files = f.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isFile();
            }
        });
        if (files == null) new ArrayList<>();
        for (int x = 0; x < files.length; x++) {
            FileHandler handler = new FileHandler();
            try {
               UTXO utxo = (UTXO) handler.readObject(files[x].getPath());
               if (utxo != null) {
                   if (!utxo.getAddress().equals(address)) continue;
                   TransactionInOut txn_in = new TransactionInOut(Long.valueOf(utxo.getTimestampIn()),utxo.getPreviousHash(),utxo.getIndex(),utxo.toFloat());
                   if (localInOuts.contains(txn_in)) {
                       localInOuts.get(localInOuts.lastIndexOf(txn_in)).addAmount(utxo.toFloat());
                   } else {
                       localInOuts.add(txn_in);
                   }
               }
            } catch (Exception e) {}
        }
        return localInOuts;
    }
    
    public ArrayList<TransactionInOut> getTransactionsPendingIn(String address, ArrayList<TransactionInOut> localInOuts) {
        File f = new File(session.getPath() + "/pending/");
        File[] files = f.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isFile();
            }
        });
        if (files == null) new ArrayList<>();
        for (File file : files) {
            FileHandler handler = new FileHandler();
            try {
                Object object = handler.readObject(file.getPath());
                if (object instanceof Transaction) {
                    for (int y = 0; y < ((Transaction) object).getOutputs().size(); y++) {
                        TransactionOutput output = ((Transaction) object).getOutputs().get(y);
                        if (!output.address.equals(address)) continue;
                        TransactionInOut txn_in = new TransactionInOut(Long.valueOf(((Transaction) object).getTimestamp()),((Transaction) object).getHash(),y,output.value);
                        if (localInOuts.contains(txn_in)) {
                            localInOuts.get(localInOuts.lastIndexOf(txn_in)).addAmount(output.value);
                        } else {
                            localInOuts.add(txn_in);
                        }
                        localInOuts.get(localInOuts.lastIndexOf(txn_in)).setType(2);
                    }
                }
            }catch (IOException | ClassNotFoundException | NumberFormatException e) {}
        }
        return localInOuts;
    }
    
    public ArrayList<TransactionInOut> getTransactionsPendingOut(String address, ArrayList<TransactionInOut> localInOuts) {
        File f = new File(session.getPath() + "/pending/");
        File[] files = f.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isFile();
            }
        });
        if (files == null) new ArrayList<>();
        for (File file : files) {
            FileHandler handler = new FileHandler();
            try {
                Object object = handler.readObject(file.getPath());
                if (object instanceof Transaction) {
                    for (int y = 0; y < ((Transaction) object).getInputs().size(); y++) {
                        TransactionInput input = ((Transaction) object).getInputs().get(y);
                        UTXO utxo = (UTXO) handler.readObject(this.session.getPath() + "/utxos/" + input.previousTxnHash + "|" + input.outputIndex);
                        if (utxo != null) {
                            if (!utxo.getAddress().equals(address)) continue;
                            TransactionInOut txn_out = new TransactionInOut(Long.valueOf(((Transaction) object).getTimestamp()),((Transaction) object).getHash(),utxo.getIndex(),utxo.toFloat().negate());
                            if (localInOuts.contains(txn_out)) {
                                localInOuts.get(localInOuts.lastIndexOf(txn_out)).addAmount(utxo.toFloat().negate());
                            } else {
                                localInOuts.add(txn_out);
                            }
                            localInOuts.get(localInOuts.lastIndexOf(txn_out)).setType(3);
                        }
                    }
                }
            }catch (IOException | ClassNotFoundException | NumberFormatException e) {}
        }
        return localInOuts;
    }
    
}
