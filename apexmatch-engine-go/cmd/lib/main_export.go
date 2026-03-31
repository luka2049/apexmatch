//export engine_get_depth_json
func engine_get_depth_json(symbolPtr *C.char, levels C.int) *C.char {
	if globalEngine == nil {
		return C.CString(`{"symbol":"","bids":[],"asks":[]}`)
	}
	symbol := C.GoString(symbolPtr)
	depth := globalEngine.GetMarketDepth(symbol, int(levels))
	depthJSON, _ := json.Marshal(depth)
	return C.CString(string(depthJSON))
}

//export engine_free_string
func engine_free_string(ptr *C.char) {
	C.free(unsafe.Pointer(ptr))
}

func main() {}
