/* Generated By:JavaCC: Do not edit this line. ParserTokenManager.java */
package org.osql.parser;
import java.io.File;
import java.io.FileInputStream;
import java.util.Vector;
import org.osql.*;

public class ParserTokenManager implements ParserConstants
{
  public  java.io.PrintStream debugStream = System.out;
  public  void setDebugStream(java.io.PrintStream ds) { debugStream = ds; }
private final int jjStopStringLiteralDfa_0(int pos, long active0)
{
   switch (pos)
   {
      case 0:
         if ((active0 & 0x8800L) != 0L)
            return 2;
         if ((active0 & 0x23fe0700L) != 0L)
         {
            jjmatchedKind = 32;
            return 5;
         }
         return -1;
      case 1:
         if ((active0 & 0x23fc0300L) != 0L)
         {
            jjmatchedKind = 32;
            jjmatchedPos = 1;
            return 5;
         }
         if ((active0 & 0x20400L) != 0L)
            return 5;
         return -1;
      case 2:
         if ((active0 & 0x23fc0000L) != 0L)
         {
            jjmatchedKind = 32;
            jjmatchedPos = 2;
            return 5;
         }
         if ((active0 & 0x300L) != 0L)
            return 5;
         return -1;
      case 3:
         if ((active0 & 0x580000L) != 0L)
            return 5;
         if ((active0 & 0x23a40000L) != 0L)
         {
            jjmatchedKind = 32;
            jjmatchedPos = 3;
            return 5;
         }
         return -1;
      case 4:
         if ((active0 & 0x2200000L) != 0L)
            return 5;
         if ((active0 & 0x21840000L) != 0L)
         {
            jjmatchedKind = 32;
            jjmatchedPos = 4;
            return 5;
         }
         return -1;
      case 5:
         if ((active0 & 0x1800000L) != 0L)
            return 5;
         if ((active0 & 0x40000L) != 0L)
         {
            jjmatchedKind = 32;
            jjmatchedPos = 5;
            return 5;
         }
         return -1;
      default :
         return -1;
   }
}
private final int jjStartNfa_0(int pos, long active0)
{
   return jjMoveNfa_0(jjStopStringLiteralDfa_0(pos, active0), pos + 1);
}
private final int jjStopAtPos(int pos, int kind)
{
   jjmatchedKind = kind;
   jjmatchedPos = pos;
   return pos + 1;
}
private final int jjStartNfaWithStates_0(int pos, int kind, int state)
{
   jjmatchedKind = kind;
   jjmatchedPos = pos;
   try { curChar = input_stream.readChar(); }
   catch(java.io.IOException e) { return pos + 1; }
   return jjMoveNfa_0(state, pos + 1);
}
private final int jjMoveStringLiteralDfa0_0()
{
   switch(curChar)
   {
      case 39:
         return jjStopAtPos(0, 26);
      case 40:
         return jjStopAtPos(0, 6);
      case 41:
         return jjStopAtPos(0, 7);
      case 42:
         return jjStopAtPos(0, 41);
      case 43:
         return jjStopAtPos(0, 39);
      case 44:
         return jjStopAtPos(0, 31);
      case 45:
         return jjStopAtPos(0, 40);
      case 46:
         return jjStopAtPos(0, 30);
      case 47:
         return jjStopAtPos(0, 42);
      case 60:
         jjmatchedKind = 15;
         return jjMoveStringLiteralDfa1_0(0x800L);
      case 61:
         return jjStopAtPos(0, 14);
      case 62:
         jjmatchedKind = 16;
         return jjMoveStringLiteralDfa1_0(0x1000L);
      case 65:
      case 97:
         return jjMoveStringLiteralDfa1_0(0x200L);
      case 66:
      case 98:
         return jjMoveStringLiteralDfa1_0(0x1040000L);
      case 69:
      case 101:
         return jjMoveStringLiteralDfa1_0(0x800000L);
      case 70:
      case 102:
         return jjMoveStringLiteralDfa1_0(0x200000L);
      case 73:
      case 105:
         return jjMoveStringLiteralDfa1_0(0x20000L);
      case 76:
      case 108:
         return jjMoveStringLiteralDfa1_0(0x80000L);
      case 78:
      case 110:
         return jjMoveStringLiteralDfa1_0(0x400100L);
      case 79:
      case 111:
         return jjMoveStringLiteralDfa1_0(0x400L);
      case 83:
      case 115:
         return jjMoveStringLiteralDfa1_0(0x20000000L);
      case 84:
      case 116:
         return jjMoveStringLiteralDfa1_0(0x100000L);
      case 86:
      case 118:
         return jjMoveStringLiteralDfa1_0(0x2000000L);
      default :
         return jjMoveNfa_0(1, 0);
   }
}
private final int jjMoveStringLiteralDfa1_0(long active0)
{
   try { curChar = input_stream.readChar(); }
   catch(java.io.IOException e) {
      jjStopStringLiteralDfa_0(0, active0);
      return 1;
   }
   switch(curChar)
   {
      case 61:
         if ((active0 & 0x800L) != 0L)
            return jjStopAtPos(1, 11);
         else if ((active0 & 0x1000L) != 0L)
            return jjStopAtPos(1, 12);
         break;
      case 65:
      case 97:
         return jjMoveStringLiteralDfa2_0(active0, 0x2200000L);
      case 69:
      case 101:
         return jjMoveStringLiteralDfa2_0(active0, 0x40000L);
      case 73:
      case 105:
         return jjMoveStringLiteralDfa2_0(active0, 0x1080000L);
      case 78:
      case 110:
         if ((active0 & 0x20000L) != 0L)
            return jjStartNfaWithStates_0(1, 17, 5);
         return jjMoveStringLiteralDfa2_0(active0, 0x200L);
      case 79:
      case 111:
         return jjMoveStringLiteralDfa2_0(active0, 0x100L);
      case 82:
      case 114:
         if ((active0 & 0x400L) != 0L)
            return jjStartNfaWithStates_0(1, 10, 5);
         return jjMoveStringLiteralDfa2_0(active0, 0x100000L);
      case 83:
      case 115:
         return jjMoveStringLiteralDfa2_0(active0, 0x800000L);
      case 85:
      case 117:
         return jjMoveStringLiteralDfa2_0(active0, 0x20400000L);
      default :
         break;
   }
   return jjStartNfa_0(0, active0);
}
private final int jjMoveStringLiteralDfa2_0(long old0, long active0)
{
   if (((active0 &= old0)) == 0L)
      return jjStartNfa_0(0, old0); 
   try { curChar = input_stream.readChar(); }
   catch(java.io.IOException e) {
      jjStopStringLiteralDfa_0(1, active0);
      return 2;
   }
   switch(curChar)
   {
      case 67:
      case 99:
         return jjMoveStringLiteralDfa3_0(active0, 0x800000L);
      case 68:
      case 100:
         if ((active0 & 0x200L) != 0L)
            return jjStartNfaWithStates_0(2, 9, 5);
         break;
      case 75:
      case 107:
         return jjMoveStringLiteralDfa3_0(active0, 0x80000L);
      case 76:
      case 108:
         return jjMoveStringLiteralDfa3_0(active0, 0x2600000L);
      case 80:
      case 112:
         return jjMoveStringLiteralDfa3_0(active0, 0x20000000L);
      case 84:
      case 116:
         if ((active0 & 0x100L) != 0L)
            return jjStartNfaWithStates_0(2, 8, 5);
         return jjMoveStringLiteralDfa3_0(active0, 0x1040000L);
      case 85:
      case 117:
         return jjMoveStringLiteralDfa3_0(active0, 0x100000L);
      default :
         break;
   }
   return jjStartNfa_0(1, active0);
}
private final int jjMoveStringLiteralDfa3_0(long old0, long active0)
{
   if (((active0 &= old0)) == 0L)
      return jjStartNfa_0(1, old0); 
   try { curChar = input_stream.readChar(); }
   catch(java.io.IOException e) {
      jjStopStringLiteralDfa_0(2, active0);
      return 3;
   }
   switch(curChar)
   {
      case 65:
      case 97:
         return jjMoveStringLiteralDfa4_0(active0, 0x1800000L);
      case 69:
      case 101:
         if ((active0 & 0x80000L) != 0L)
            return jjStartNfaWithStates_0(3, 19, 5);
         else if ((active0 & 0x100000L) != 0L)
            return jjStartNfaWithStates_0(3, 20, 5);
         return jjMoveStringLiteralDfa4_0(active0, 0x20000000L);
      case 76:
      case 108:
         if ((active0 & 0x400000L) != 0L)
            return jjStartNfaWithStates_0(3, 22, 5);
         break;
      case 83:
      case 115:
         return jjMoveStringLiteralDfa4_0(active0, 0x200000L);
      case 85:
      case 117:
         return jjMoveStringLiteralDfa4_0(active0, 0x2000000L);
      case 87:
      case 119:
         return jjMoveStringLiteralDfa4_0(active0, 0x40000L);
      default :
         break;
   }
   return jjStartNfa_0(2, active0);
}
private final int jjMoveStringLiteralDfa4_0(long old0, long active0)
{
   if (((active0 &= old0)) == 0L)
      return jjStartNfa_0(2, old0); 
   try { curChar = input_stream.readChar(); }
   catch(java.io.IOException e) {
      jjStopStringLiteralDfa_0(3, active0);
      return 4;
   }
   switch(curChar)
   {
      case 69:
      case 101:
         if ((active0 & 0x200000L) != 0L)
            return jjStartNfaWithStates_0(4, 21, 5);
         else if ((active0 & 0x2000000L) != 0L)
            return jjStartNfaWithStates_0(4, 25, 5);
         return jjMoveStringLiteralDfa5_0(active0, 0x40000L);
      case 78:
      case 110:
         return jjMoveStringLiteralDfa5_0(active0, 0x1000000L);
      case 80:
      case 112:
         return jjMoveStringLiteralDfa5_0(active0, 0x800000L);
      case 82:
      case 114:
         return jjMoveStringLiteralDfa5_0(active0, 0x20000000L);
      default :
         break;
   }
   return jjStartNfa_0(3, active0);
}
private final int jjMoveStringLiteralDfa5_0(long old0, long active0)
{
   if (((active0 &= old0)) == 0L)
      return jjStartNfa_0(3, old0); 
   try { curChar = input_stream.readChar(); }
   catch(java.io.IOException e) {
      jjStopStringLiteralDfa_0(4, active0);
      return 5;
   }
   switch(curChar)
   {
      case 46:
         if ((active0 & 0x20000000L) != 0L)
            return jjStopAtPos(5, 29);
         break;
      case 68:
      case 100:
         if ((active0 & 0x1000000L) != 0L)
            return jjStartNfaWithStates_0(5, 24, 5);
         break;
      case 69:
      case 101:
         if ((active0 & 0x800000L) != 0L)
            return jjStartNfaWithStates_0(5, 23, 5);
         return jjMoveStringLiteralDfa6_0(active0, 0x40000L);
      default :
         break;
   }
   return jjStartNfa_0(4, active0);
}
private final int jjMoveStringLiteralDfa6_0(long old0, long active0)
{
   if (((active0 &= old0)) == 0L)
      return jjStartNfa_0(4, old0); 
   try { curChar = input_stream.readChar(); }
   catch(java.io.IOException e) {
      jjStopStringLiteralDfa_0(5, active0);
      return 6;
   }
   switch(curChar)
   {
      case 78:
      case 110:
         if ((active0 & 0x40000L) != 0L)
            return jjStartNfaWithStates_0(6, 18, 5);
         break;
      default :
         break;
   }
   return jjStartNfa_0(5, active0);
}
private final void jjCheckNAdd(int state)
{
   if (jjrounds[state] != jjround)
   {
      jjstateSet[jjnewStateCnt++] = state;
      jjrounds[state] = jjround;
   }
}
private final void jjAddStates(int start, int end)
{
   do {
      jjstateSet[jjnewStateCnt++] = jjnextStates[start];
   } while (start++ != end);
}
private final void jjCheckNAddTwoStates(int state1, int state2)
{
   jjCheckNAdd(state1);
   jjCheckNAdd(state2);
}
private final void jjCheckNAddStates(int start, int end)
{
   do {
      jjCheckNAdd(jjnextStates[start]);
   } while (start++ != end);
}
private final void jjCheckNAddStates(int start)
{
   jjCheckNAdd(jjnextStates[start]);
   jjCheckNAdd(jjnextStates[start + 1]);
}
private final int jjMoveNfa_0(int startState, int curPos)
{
   int[] nextStates;
   int startsAt = 0;
   jjnewStateCnt = 15;
   int i = 1;
   jjstateSet[0] = startState;
   int j, kind = 0x7fffffff;
   for (;;)
   {
      if (++jjround == 0x7fffffff)
         ReInitRounds();
      if (curChar < 64)
      {
         long l = 1L << curChar;
         MatchLoop: do
         {
            switch(jjstateSet[--i])
            {
               case 1:
                  if ((0x3fe000000000000L & l) != 0L)
                  {
                     if (kind > 36)
                        kind = 36;
                     jjCheckNAddStates(0, 2);
                  }
                  else if (curChar == 48)
                  {
                     if (kind > 36)
                        kind = 36;
                     jjCheckNAdd(7);
                  }
                  else if (curChar == 36)
                  {
                     if (kind > 32)
                        kind = 32;
                     jjCheckNAdd(5);
                  }
                  else if (curChar == 60)
                     jjstateSet[jjnewStateCnt++] = 2;
                  else if (curChar == 33)
                     jjstateSet[jjnewStateCnt++] = 0;
                  break;
               case 0:
                  if (curChar == 61)
                     kind = 13;
                  break;
               case 2:
                  if (curChar == 62)
                     kind = 13;
                  break;
               case 3:
                  if (curChar == 60)
                     jjstateSet[jjnewStateCnt++] = 2;
                  break;
               case 4:
                  if (curChar != 36)
                     break;
                  if (kind > 32)
                     kind = 32;
                  jjCheckNAdd(5);
                  break;
               case 5:
                  if ((0x3ff001000000000L & l) == 0L)
                     break;
                  if (kind > 32)
                     kind = 32;
                  jjCheckNAdd(5);
                  break;
               case 6:
                  if (curChar != 48)
                     break;
                  if (kind > 36)
                     kind = 36;
                  jjCheckNAdd(7);
                  break;
               case 7:
                  if (curChar == 46)
                     jjCheckNAdd(8);
                  break;
               case 8:
                  if ((0x3ff000000000000L & l) == 0L)
                     break;
                  if (kind > 37)
                     kind = 37;
                  jjCheckNAddTwoStates(8, 9);
                  break;
               case 10:
                  if ((0x280000000000L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 11;
                  break;
               case 11:
                  if ((0x3ff000000000000L & l) != 0L && kind > 37)
                     kind = 37;
                  break;
               case 12:
                  if ((0x3fe000000000000L & l) == 0L)
                     break;
                  if (kind > 36)
                     kind = 36;
                  jjCheckNAddStates(0, 2);
                  break;
               case 13:
                  if ((0x3ff000000000000L & l) == 0L)
                     break;
                  if (kind > 36)
                     kind = 36;
                  jjCheckNAdd(13);
                  break;
               case 14:
                  if ((0x3ff000000000000L & l) != 0L)
                     jjCheckNAddTwoStates(14, 7);
                  break;
               default : break;
            }
         } while(i != startsAt);
      }
      else if (curChar < 128)
      {
         long l = 1L << (curChar & 077);
         MatchLoop: do
         {
            switch(jjstateSet[--i])
            {
               case 1:
               case 5:
                  if ((0x7fffffe87fffffeL & l) == 0L)
                     break;
                  if (kind > 32)
                     kind = 32;
                  jjCheckNAdd(5);
                  break;
               case 9:
                  if ((0x2000000020L & l) != 0L)
                     jjAddStates(3, 4);
                  break;
               default : break;
            }
         } while(i != startsAt);
      }
      else
      {
         int i2 = (curChar & 0xff) >> 6;
         long l2 = 1L << (curChar & 077);
         MatchLoop: do
         {
            switch(jjstateSet[--i])
            {
               default : break;
            }
         } while(i != startsAt);
      }
      if (kind != 0x7fffffff)
      {
         jjmatchedKind = kind;
         jjmatchedPos = curPos;
         kind = 0x7fffffff;
      }
      ++curPos;
      if ((i = jjnewStateCnt) == (startsAt = 15 - (jjnewStateCnt = startsAt)))
         return curPos;
      try { curChar = input_stream.readChar(); }
      catch(java.io.IOException e) { return curPos; }
   }
}
private final int jjStopStringLiteralDfa_1(int pos, long active0)
{
   switch (pos)
   {
      default :
         return -1;
   }
}
private final int jjStartNfa_1(int pos, long active0)
{
   return jjMoveNfa_1(jjStopStringLiteralDfa_1(pos, active0), pos + 1);
}
private final int jjStartNfaWithStates_1(int pos, int kind, int state)
{
   jjmatchedKind = kind;
   jjmatchedPos = pos;
   try { curChar = input_stream.readChar(); }
   catch(java.io.IOException e) { return pos + 1; }
   return jjMoveNfa_1(state, pos + 1);
}
private final int jjMoveStringLiteralDfa0_1()
{
   switch(curChar)
   {
      case 39:
         return jjStopAtPos(0, 28);
      default :
         return jjMoveNfa_1(0, 0);
   }
}
static final long[] jjbitVec0 = {
   0x0L, 0x0L, 0xffffffffffffffffL, 0xffffffffffffffffL
};
private final int jjMoveNfa_1(int startState, int curPos)
{
   int[] nextStates;
   int startsAt = 0;
   jjnewStateCnt = 1;
   int i = 1;
   jjstateSet[0] = startState;
   int j, kind = 0x7fffffff;
   for (;;)
   {
      if (++jjround == 0x7fffffff)
         ReInitRounds();
      if (curChar < 64)
      {
         long l = 1L << curChar;
         MatchLoop: do
         {
            switch(jjstateSet[--i])
            {
               case 0:
                  if ((0xffffff7fffffffffL & l) == 0L)
                     break;
                  kind = 27;
                  jjstateSet[jjnewStateCnt++] = 0;
                  break;
               default : break;
            }
         } while(i != startsAt);
      }
      else if (curChar < 128)
      {
         long l = 1L << (curChar & 077);
         MatchLoop: do
         {
            switch(jjstateSet[--i])
            {
               case 0:
                  kind = 27;
                  jjstateSet[jjnewStateCnt++] = 0;
                  break;
               default : break;
            }
         } while(i != startsAt);
      }
      else
      {
         int i2 = (curChar & 0xff) >> 6;
         long l2 = 1L << (curChar & 077);
         MatchLoop: do
         {
            switch(jjstateSet[--i])
            {
               case 0:
                  if ((jjbitVec0[i2] & l2) == 0L)
                     break;
                  if (kind > 27)
                     kind = 27;
                  jjstateSet[jjnewStateCnt++] = 0;
                  break;
               default : break;
            }
         } while(i != startsAt);
      }
      if (kind != 0x7fffffff)
      {
         jjmatchedKind = kind;
         jjmatchedPos = curPos;
         kind = 0x7fffffff;
      }
      ++curPos;
      if ((i = jjnewStateCnt) == (startsAt = 1 - (jjnewStateCnt = startsAt)))
         return curPos;
      try { curChar = input_stream.readChar(); }
      catch(java.io.IOException e) { return curPos; }
   }
}
static final int[] jjnextStates = {
   13, 14, 7, 10, 11, 
};
public static final String[] jjstrLiteralImages = {
"", null, null, null, null, null, "\50", "\51", null, null, null, "\74\75", 
"\76\75", null, "\75", "\74", "\76", null, null, null, null, null, null, null, null, 
null, null, null, null, null, "\56", "\54", null, null, null, null, null, null, null, 
"\53", "\55", "\52", "\57", };
public static final String[] lexStateNames = {
   "DEFAULT", 
   "_STRING", 
};
public static final int[] jjnewLexState = {
   -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 
   -1, 1, -1, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 
};
static final long[] jjtoToken = {
   0x7b1ebffffc1L, 
};
static final long[] jjtoSkip = {
   0x1400003eL, 
};
protected SimpleCharStream input_stream;
private final int[] jjrounds = new int[15];
private final int[] jjstateSet = new int[30];
protected char curChar;
public ParserTokenManager(SimpleCharStream stream){
   if (SimpleCharStream.staticFlag)
      throw new Error("ERROR: Cannot use a static CharStream class with a non-static lexical analyzer.");
   input_stream = stream;
}
public ParserTokenManager(SimpleCharStream stream, int lexState){
   this(stream);
   SwitchTo(lexState);
}
public void ReInit(SimpleCharStream stream)
{
   jjmatchedPos = jjnewStateCnt = 0;
   curLexState = defaultLexState;
   input_stream = stream;
   ReInitRounds();
}
private final void ReInitRounds()
{
   int i;
   jjround = 0x80000001;
   for (i = 15; i-- > 0;)
      jjrounds[i] = 0x80000000;
}
public void ReInit(SimpleCharStream stream, int lexState)
{
   ReInit(stream);
   SwitchTo(lexState);
}
public void SwitchTo(int lexState)
{
   if (lexState >= 2 || lexState < 0)
      throw new TokenMgrError("Error: Ignoring invalid lexical state : " + lexState + ". State unchanged.", TokenMgrError.INVALID_LEXICAL_STATE);
   else
      curLexState = lexState;
}

protected Token jjFillToken()
{
   Token t = Token.newToken(jjmatchedKind);
   t.kind = jjmatchedKind;
   String im = jjstrLiteralImages[jjmatchedKind];
   t.image = (im == null) ? input_stream.GetImage() : im;
   t.beginLine = input_stream.getBeginLine();
   t.beginColumn = input_stream.getBeginColumn();
   t.endLine = input_stream.getEndLine();
   t.endColumn = input_stream.getEndColumn();
   return t;
}

int curLexState = 0;
int defaultLexState = 0;
int jjnewStateCnt;
int jjround;
int jjmatchedPos;
int jjmatchedKind;

public Token getNextToken() 
{
  int kind;
  Token specialToken = null;
  Token matchedToken;
  int curPos = 0;

  EOFLoop :
  for (;;)
  {   
   try   
   {     
      curChar = input_stream.BeginToken();
   }     
   catch(java.io.IOException e)
   {        
      jjmatchedKind = 0;
      matchedToken = jjFillToken();
      return matchedToken;
   }

   switch(curLexState)
   {
     case 0:
       try { input_stream.backup(0);
          while (curChar <= 32 && (0x100003600L & (1L << curChar)) != 0L)
             curChar = input_stream.BeginToken();
       }
       catch (java.io.IOException e1) { continue EOFLoop; }
       jjmatchedKind = 0x7fffffff;
       jjmatchedPos = 0;
       curPos = jjMoveStringLiteralDfa0_0();
       break;
     case 1:
       jjmatchedKind = 0x7fffffff;
       jjmatchedPos = 0;
       curPos = jjMoveStringLiteralDfa0_1();
       break;
   }
     if (jjmatchedKind != 0x7fffffff)
     {
        if (jjmatchedPos + 1 < curPos)
           input_stream.backup(curPos - jjmatchedPos - 1);
        if ((jjtoToken[jjmatchedKind >> 6] & (1L << (jjmatchedKind & 077))) != 0L)
        {
           matchedToken = jjFillToken();
       if (jjnewLexState[jjmatchedKind] != -1)
         curLexState = jjnewLexState[jjmatchedKind];
           return matchedToken;
        }
        else
        {
         if (jjnewLexState[jjmatchedKind] != -1)
           curLexState = jjnewLexState[jjmatchedKind];
           continue EOFLoop;
        }
     }
     int error_line = input_stream.getEndLine();
     int error_column = input_stream.getEndColumn();
     String error_after = null;
     boolean EOFSeen = false;
     try { input_stream.readChar(); input_stream.backup(1); }
     catch (java.io.IOException e1) {
        EOFSeen = true;
        error_after = curPos <= 1 ? "" : input_stream.GetImage();
        if (curChar == '\n' || curChar == '\r') {
           error_line++;
           error_column = 0;
        }
        else
           error_column++;
     }
     if (!EOFSeen) {
        input_stream.backup(1);
        error_after = curPos <= 1 ? "" : input_stream.GetImage();
     }
     throw new TokenMgrError(EOFSeen, curLexState, error_line, error_column, error_after, curChar, TokenMgrError.LEXICAL_ERROR);
  }
}

}
