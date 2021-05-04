package com.ahumada.fuse.enums;

public enum DocEstadoEnum {
	
	VIGENTE			(true, 	"V"),
	BLOQUEADO		(true, 	"B"),
	NO_EMITIDO		(true, 	"N");
	
	private final boolean valido;
	private final String estado;
	
	private DocEstadoEnum(boolean valido, String estado) {
		this.valido = valido;
		this.estado = estado;
	}

	public boolean isValido() {
		return valido;
	}

	public String getEstado() {
		return estado;
	}
	
	
}
