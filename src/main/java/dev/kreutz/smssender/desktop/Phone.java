package dev.kreutz.smssender.desktop;

import dev.kreutz.smssender.shared.*;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Set;
import java.util.TreeSet;

/**
 * Simple wrapper for a phone connected
 */
public class Phone {

    /**
     * OutputStream of the phone
     */
    private final ObjectOutputStream writer;

    /**
     * InputStream of the phone
     */
    private final ObjectInputStream reader;

    /**
     * Name of the phone
     */
    private final String name;

    /**
     * All the groups of the phone
     */
    private final Set<String> groups;

    /**
     * If the phone is currently sending
     */
    private boolean busy = false;

    /**
     * If the phone got cancelled
     */
    private boolean cancelled = false;

    public Phone(Socket socket) throws Exception {
        writer = new ObjectOutputStream(socket.getOutputStream());
        reader = new ObjectInputStream(socket.getInputStream());

        writer.writeObject(new NameRequest());
        NameResponse nameResponse = (NameResponse) reader.readObject();
        name = nameResponse.getName();

        writer.writeObject(new GroupsRequest());
        GroupsResponse groupsResponse = (GroupsResponse) reader.readObject();
        groups = groupsResponse.getGroups();
    }

    public String getName() {
        return name;
    }

    public Set<String> getGroups() {
        return groups;
    }

    public boolean busy() {
        return busy;
    }

    public boolean cancelled() {
        return cancelled;
    }

    public void setBusy(boolean busy) {
        this.busy = busy;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    /**
     * Gets all numbers of the groups
     *
     * @param groups the groups to get the numbers from
     * @return the numbers
     */
    public Set<String> getNumbers(Set<String> groups) {
        try {
            writer.writeObject(new NumbersRequest(groups));
            NumbersResponse numbersResponse = (NumbersResponse) reader.readObject();
            return numbersResponse.getNumbers();
        } catch (Exception e) {
            return new TreeSet<>();
        }
    }

    /**
     * Let the phone know which text to send
     *
     * @param text the text for the phone to safe
     * @return true if it succeeded
     */
    public boolean setText(String text) {
        try {
            writer.writeObject(new SetTextRequest(text));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Make phone send sms to number
     *
     * @param number The number to send the sms to
     * @return True if succeeded
     */
    public boolean sendSms(String number) {
        try {
            writer.writeObject(new SendSmsRequest(number));
            SendSmsResponse sendSmsResponse = (SendSmsResponse) reader.readObject();
            return sendSmsResponse.isOk();
        } catch (Exception e) {
            return false;
        }
    }


    @Override
    public String toString() {
        return name;
    }
}
