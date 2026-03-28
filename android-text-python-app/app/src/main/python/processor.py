import ast
import io
import time
import traceback
from contextlib import redirect_stderr, redirect_stdout


_CANCELLED_REQUESTS = set()

_SAFE_BUILTINS = {
    "abs": abs,
    "all": all,
    "any": any,
    "bool": bool,
    "dict": dict,
    "enumerate": enumerate,
    "float": float,
    "int": int,
    "len": len,
    "list": list,
    "max": max,
    "min": min,
    "print": print,
    "range": range,
    "round": round,
    "set": set,
    "sorted": sorted,
    "str": str,
    "sum": sum,
    "tuple": tuple,
    "zip": zip,
    "Exception": Exception,
    "ValueError": ValueError,
    "TypeError": TypeError,
    "RuntimeError": RuntimeError,
}


class CancelledByHost(Exception):
    pass


def cancel_request(request_id: str) -> None:
    if request_id:
        _CANCELLED_REQUESTS.add(str(request_id))


def clear_request(request_id: str) -> None:
    if request_id:
        _CANCELLED_REQUESTS.discard(str(request_id))


def _ensure_not_cancelled(request_id: str) -> None:
    if request_id and str(request_id) in _CANCELLED_REQUESTS:
        raise CancelledByHost("Cancelled by host token")


def _sleep_with_poll(total_delay_ms: int, request_id: str) -> None:
    remaining = max(0, int(total_delay_ms))
    step_ms = 50

    while remaining > 0:
        _ensure_not_cancelled(request_id)
        chunk = min(step_ms, remaining)
        time.sleep(chunk / 1000.0)
        remaining -= chunk


def _execute_python_snippet(text: str, request_id: str) -> dict:
    _ensure_not_cancelled(request_id)

    tree = ast.parse(text, mode="exec")
    body = list(tree.body)
    last_expr = None
    if body and isinstance(body[-1], ast.Expr):
        last_expr = body.pop().value

    globals_ns = {"__builtins__": _SAFE_BUILTINS}
    locals_ns = {}

    if body:
        module = ast.Module(body=body, type_ignores=[])
        ast.fix_missing_locations(module)
        exec(compile(module, "<process_text>", "exec"), globals_ns, locals_ns)
        _ensure_not_cancelled(request_id)

    result_value = None
    if last_expr is not None:
        expression = ast.Expression(last_expr)
        ast.fix_missing_locations(expression)
        result_value = eval(compile(expression, "<process_text>", "eval"), globals_ns, locals_ns)
        _ensure_not_cancelled(request_id)
    elif "result" in locals_ns:
        result_value = locals_ns["result"]

    if result_value is None:
        return {"text": "python_ok"}

    return {"text": str(result_value)}


def _do_process_text(
    text: str,
    request_id: str,
    debug_delay_ms: int = 0,
    debug_fail: bool = False,
) -> dict:
    _ensure_not_cancelled(request_id)
    _sleep_with_poll(debug_delay_ms, request_id)

    if debug_fail:
        raise ValueError("debug_fail requested")

    # Always treat selected content as Python code.
    # Non-code text should fail with a normal Python exception payload.
    return _execute_python_snippet(text, request_id)


def process_text(
    text: str,
    request_id: str = "",
    debug_delay_ms: int = 0,
    debug_fail: bool = False,
) -> dict:
    out = io.StringIO()
    err = io.StringIO()
    result = None
    error_code = None
    message = "ok"
    error = None
    tb = None
    request_id = str(request_id or "")

    with redirect_stdout(out), redirect_stderr(err):
        try:
            result = _do_process_text(text, request_id, int(debug_delay_ms), bool(debug_fail))
            status = "success"
        except CancelledByHost as exc:
            status = "cancelled"
            error_code = "PYTHON_CANCELLED"
            message = str(exc)
            error = str(exc)
        except Exception as exc:
            status = "error"
            error_code = "PYTHON_RUNTIME_ERROR"
            message = str(exc)
            error = f"{type(exc).__name__}: {exc}"
            tb = traceback.format_exc()

    return {
        "requestId": request_id,
        "status": status,
        "errorCode": error_code,
        "message": message,
        "result": result,
        "stdout": out.getvalue(),
        "stderr": err.getvalue(),
        "error": error,
        "traceback": tb,
    }
