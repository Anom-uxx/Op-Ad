package org.anomuxx.core.commands;

import net.labymod.api.Laby;
import net.labymod.api.client.chat.command.Command;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.format.NamedTextColor;
import org.anomuxx.core.listener.ChatListener;

public class OpAdCommand extends Command {

  private final ChatListener chatListener;

  public OpAdCommand(ChatListener chatListener) {
    super("opwerbung");
    this.chatListener = chatListener;
  }

  @Override
  public boolean execute(String prefix, String[] arguments) {
    if (arguments.length < 2) {
      this.error();
      return true;
    }

    int index;
    try {
      index = Integer.parseInt(arguments[1]);
    } catch (NumberFormatException exception) {
      this.error();
      return true;
    }

    if (arguments[0].equalsIgnoreCase("confirm")) {
      if (!this.chatListener.requestConfirmation(index)) {
        this.error();
      }
      return true;
    }

    if (arguments[0].equalsIgnoreCase("send")) {
      if (!this.chatListener.sendAnyway(index)) {
        this.error();
      }
      return true;
    }

    if (arguments[0].equalsIgnoreCase("sendready")) {
      if (!this.chatListener.sendReady(index)) {
        this.error();
      }
      return true;
    }

    this.error();
    return true;
  }

  private void error() {
    Laby.labyAPI().minecraft().chatExecutor().displayClientMessage(
        Component.translatable("opwerbung.messages.invalid", NamedTextColor.RED));
  }
}
