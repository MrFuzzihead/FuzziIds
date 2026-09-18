package com.mrfuzzihead.fuzziids;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

import com.mrfuzzihead.fuzziids.csv.IdDumpManager;

/** /fuzziids dump - re-writes all ID report CSVs on demand. */
public class DumpIdsCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "fuzziids";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/fuzziids dump";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (!Config.mainEnable) {
            sender.addChatMessage(
                new ChatComponentText(
                    "FuzziIds is disabled (mainEnable=false in config/" + FuzziIds.MODID
                        + "/"
                        + FuzziIds.MODID
                        + ".cfg)"));
            return;
        }
        IdDumpManager.dump();
        sender.addChatMessage(
            new ChatComponentText("FuzziIds wrote ID reports to " + Config.configDir.getAbsolutePath()));
    }
}
