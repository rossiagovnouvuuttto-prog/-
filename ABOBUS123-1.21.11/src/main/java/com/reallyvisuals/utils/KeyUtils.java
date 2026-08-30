package com.reallyvisuals.utils;

import org.lwjgl.glfw.GLFW;

public class KeyUtils {
   public static final int MOUSE_OFFSET = 1000;

   private KeyUtils() {
   }

   public static boolean isMouseBind(int code) {
      return code >= 1000;
   }

   public static int getMouseButton(int code) {
      return code - 1000;
   }

   public static int toMouseBind(int button) {
      return 1000 + button;
   }


   public static boolean isKeyPressedSafe(long window, int key) {
      if (window == 0L || key < 32 || key > GLFW.GLFW_KEY_LAST) {
         return false;
      }

      try {
         return GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
      } catch (Throwable ignored) {
         return false;
      }
   }

   public static boolean isMouseButtonPressedSafe(long window, int button) {
      if (window == 0L || button < 0 || button > GLFW.GLFW_MOUSE_BUTTON_LAST) {
         return false;
      }

      try {
         return GLFW.glfwGetMouseButton(window, button) == GLFW.GLFW_PRESS;
      } catch (Throwable ignored) {
         return false;
      }
   }

   public static String getShortKeyName(int code) {
      String name = getKeyName(code);
      if (name == null) {
         return "None";
      } else {
         switch (name) {
            case "BACKSPACE":
               return "BKSP";
            case "PRINTSCR":
               return "PRTSC";
            case "KPENTER":
               return "KPENT";
            case "KPEQUAL":
               return "KP=";
            case "LSHIFT":
               return "LSHFT";
            case "RSHIFT":
               return "RSHFT";
            case "LSUPER":
               return "LWIN";
            case "RSUPER":
               return "RWIN";
            case "NUMLOCK":
               return "NUMLK";
            case "SCROLL":
               return "SCRLK";
            case "DELETE":
               return "DEL";
            case "INSERT":
               return "INS";
            case "ENTER":
               return "ENTR";
            case "None":
               return "—";
            default:
               return name;
         }
      }
   }

   public static String getKeyName(int code) {
      if (code <= 0) {
         return "None";
      } else if (code >= 1000) {
         int b = code - 1000;
         switch (b) {
            case 0:
               return "LMB";
            case 1:
               return "RMB";
            case 2:
               return "MMB";
            case 3:
               return "M4";
            case 4:
               return "M5";
            case 5:
               return "M6";
            case 6:
               return "M7";
            case 7:
               return "M8";
            default:
               return "M" + (b + 1);
         }
      } else {
         String name = keyName(code);
         return name != null ? name : "KEY_" + code;
      }
   }

   private static String keyName(int key) {
      switch (key) {
         case 32:
            return "SPACE";
         case 39:
            return "'";
         case 44:
            return ",";
         case 45:
            return "-";
         case 46:
            return ".";
         case 47:
            return "/";
         case 48:
            return "0";
         case 49:
            return "1";
         case 50:
            return "2";
         case 51:
            return "3";
         case 52:
            return "4";
         case 53:
            return "5";
         case 54:
            return "6";
         case 55:
            return "7";
         case 56:
            return "8";
         case 57:
            return "9";
         case 59:
            return ";";
         case 61:
            return "=";
         case 65:
            return "A";
         case 66:
            return "B";
         case 67:
            return "C";
         case 68:
            return "D";
         case 69:
            return "E";
         case 70:
            return "F";
         case 71:
            return "G";
         case 72:
            return "H";
         case 73:
            return "I";
         case 74:
            return "J";
         case 75:
            return "K";
         case 76:
            return "L";
         case 77:
            return "M";
         case 78:
            return "N";
         case 79:
            return "O";
         case 80:
            return "P";
         case 81:
            return "Q";
         case 82:
            return "R";
         case 83:
            return "S";
         case 84:
            return "T";
         case 85:
            return "U";
         case 86:
            return "V";
         case 87:
            return "W";
         case 88:
            return "X";
         case 89:
            return "Y";
         case 90:
            return "Z";
         case 91:
            return "[";
         case 92:
            return "\\";
         case 93:
            return "]";
         case 96:
            return "`";
         case 256:
            return "ESC";
         case 257:
            return "ENTER";
         case 258:
            return "TAB";
         case 259:
            return "BACKSPACE";
         case 260:
            return "INSERT";
         case 261:
            return "DELETE";
         case 262:
            return "RIGHT";
         case 263:
            return "LEFT";
         case 264:
            return "DOWN";
         case 265:
            return "UP";
         case 266:
            return "PGUP";
         case 267:
            return "PGDN";
         case 268:
            return "HOME";
         case 269:
            return "END";
         case 280:
            return "CAPS";
         case 281:
            return "SCROLL";
         case 282:
            return "NUMLOCK";
         case 283:
            return "PRINTSCR";
         case 284:
            return "PAUSE";
         case 290:
            return "F1";
         case 291:
            return "F2";
         case 292:
            return "F3";
         case 293:
            return "F4";
         case 294:
            return "F5";
         case 295:
            return "F6";
         case 296:
            return "F7";
         case 297:
            return "F8";
         case 298:
            return "F9";
         case 299:
            return "F10";
         case 300:
            return "F11";
         case 301:
            return "F12";
         case 302:
            return "F13";
         case 303:
            return "F14";
         case 304:
            return "F15";
         case 305:
            return "F16";
         case 306:
            return "F17";
         case 307:
            return "F18";
         case 308:
            return "F19";
         case 309:
            return "F20";
         case 310:
            return "F21";
         case 311:
            return "F22";
         case 312:
            return "F23";
         case 313:
            return "F24";
         case 314:
            return "F25";
         case 320:
            return "KP0";
         case 321:
            return "KP1";
         case 322:
            return "KP2";
         case 323:
            return "KP3";
         case 324:
            return "KP4";
         case 325:
            return "KP5";
         case 326:
            return "KP6";
         case 327:
            return "KP7";
         case 328:
            return "KP8";
         case 329:
            return "KP9";
         case 330:
            return "KPDEL";
         case 331:
            return "KPDIV";
         case 332:
            return "KPMUL";
         case 333:
            return "KPSUB";
         case 334:
            return "KPADD";
         case 335:
            return "KPENTER";
         case 336:
            return "KPEQUAL";
         case 340:
            return "LSHIFT";
         case 341:
            return "LCTRL";
         case 342:
            return "LALT";
         case 343:
            return "LSUPER";
         case 344:
            return "RSHIFT";
         case 345:
            return "RCTRL";
         case 346:
            return "RALT";
         case 347:
            return "RSUPER";
         case 348:
            return "MENU";
         default:
            return key >= 32 && key <= 126 ? String.valueOf((char)key).toUpperCase() : null;
      }
   }
}
