package com.realtimetech.opack.interlanguage;

import com.realtimetech.opack.interlanguage.code.*;
import com.realtimetech.opack.value.*;

import java.util.Stack;

public class VirtualMachine {
    private CodePushField codePushField;
    private Object object;

    public void sandbox() {
        Stack<CallContext> callStack = new Stack<>();
        Stack<OpackValue> opackStack = new Stack<>();
        Stack<Object> valueStack = new Stack<>();

        while (!callStack.empty()) {
            CallContext callContext = callStack.peek();
            Code code = callContext.take();

            if (code == null) {
                callStack.pop();
            } else {
                switch (code.type) {
                    case CREATE_OPACK_OBJECT:
                        opackStack.push(new OpackObject());
                        break;
                    case CREATE_OPACK_ARRAY:
                        opackStack.push(new OpackArray());
                        break;
                    case CREATE_OPACK_NONE:
                        opackStack.push(new OpackNone());
                        break;
                    case CREATE_OPACK_BOOL:
                        opackStack.push(new OpackBool((Boolean) valueStack.pop()));
                        break;
                    case CREATE_OPACK_NUMBER:
                        opackStack.push(new OpackNumber((Number) valueStack.pop()));
                        break;
                    case CREATE_OPACK_STRING:
                        opackStack.push(new OpackString((String) valueStack.pop()));
                        break;
                    case MODIFY_OPACK_OBJECT: {
                        OpackValue v1 = opackStack.pop();
                        OpackValue v2 = opackStack.pop();
                        ((OpackObject) opackStack.peek()).put(v1, v2);
                        break;
                    }
                    case MODIFY_OPACK_OBJECT_WITH_STRING: {
                        OpackValue v1 = new OpackString(((CodeModifyOpackObjectWithString) code).string);
                        OpackValue v2 = opackStack.pop();
                        ((OpackObject) opackStack.peek()).put(v1, v2);
                        break;
                    }
                    case MODIFY_OPACK_ARRAY: {
                        int v1 = (int) valueStack.pop();
                        OpackValue v2 = opackStack.pop();
                        ((OpackArray) opackStack.peek()).set(v1, v2);
                        break;
                    }
                    case MODIFY_OPACK_ARRAY_WITH_INDEX: {
                        OpackValue v1 = opackStack.pop();
                        ((OpackArray) opackStack.peek()).set(((CodeModifyOpackArrayWithIndex) code).index, v1);
                        break;
                    }
                    case PUSH_CONST:
                        valueStack.push(((CodePushConst) code).value);
                        break;
                    case PUSH_FIELD:
                        try {
                            valueStack.push(callContext.getField(((CodePushField) code).field));
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                            // TODO: Handling this exception
                        }
                        break;
                    case CALL: {
                        Object object = valueStack.pop();
                        // TODO: Get prebuilt codes from storage
                        CallContext newCallContext = new CallContext(null, object);
                        callStack.push(newCallContext);
                        break;
                    }
                }
            }
        }
    }
}
