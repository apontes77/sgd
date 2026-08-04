#!/usr/bin/env python3
"""Compat: redireciona para scripts/carga_estrutura.py --lote masculino."""

from __future__ import annotations

import sys

from carga_estrutura import main

if __name__ == "__main__":
    # Mantém comportamento antigo quando chamado sem --lote
    argv = sys.argv[1:]
    if "--lote" not in argv:
        argv = ["--lote", "masculino", *argv]
    raise SystemExit(main(argv))
