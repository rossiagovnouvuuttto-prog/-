package com.reallyvisuals.module;

import java.util.ArrayList;
import java.util.List;

public class FriendManager {
   private static final List<FriendManager.Friend> friends = new ArrayList<>();

   public static List<FriendManager.Friend> getFriends() {
      return friends;
   }

   public static boolean isFriend(String name) {
      for (FriendManager.Friend f : friends) {
         if (f.name.equalsIgnoreCase(name)) {
            return true;
         }
      }

      return false;
   }

   public static void addFriend(String name) {
      if (name != null && !name.trim().isEmpty() && !isFriend(name)) {
         friends.add(new FriendManager.Friend(name.trim(), false, "сегодня"));
      }
   }

   public static void removeFriend(String name) {
      friends.removeIf(f -> f.name.equalsIgnoreCase(name));
   }

   public static class Friend {
      public String name;
      public boolean online;
      public String dateAdded;

      public Friend(String name, boolean online, String dateAdded) {
         this.name = name;
         this.online = online;
         this.dateAdded = dateAdded;
      }
   }
}
