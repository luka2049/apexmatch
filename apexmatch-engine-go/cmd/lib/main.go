package main

/*
#include <stdlib.h>
*/
import "C"
import (
	"encoding/json"
	"unsafe"
	"github.com/apexmatch/engine-go/pkg/engine"
	"github.com/apexmatch/engine-go/pkg/types"
)

var globalEngine *engine.MatchingEngine

//export engine_init
func engine_init(symbolPtr *C.char) {
	if globalEngine == nil {
		globalEngine = engine.NewMatchingEngine()
	}
	symbol := C.GoString(symbolPtr)
	globalEngine.Init(symbol)
}

//export engine_submit_order_json
func engine_submit_order_json(jsonPtr *C.char) *C.char {
	if globalEngine == nil {
		return C.CString(`{"rejectReason":"Engine not initialized"}`)
	}

	jsonStr := C.GoString(jsonPtr)
	var order types.Order
	if err := json.Unmarshal([]byte(jsonStr), &order); err != nil {
		return C.CString(`{"rejectReason":"Invalid JSON"}`)
	}

	result := globalEngine.SubmitOrder(&order)
	resultJSON, _ := json.Marshal(result)
	return C.CString(string(resultJSON))
}

//export engine_cancel_order
func engine_cancel_order(symbolPtr *C.char, orderID C.longlong) C.uchar {
	if globalEngine == nil {
		return 0
	}
	symbol := C.GoString(symbolPtr)
	success := globalEngine.CancelOrder(symbol, int64(orderID))
	if success {
		return 1
	}
	return 0
}
