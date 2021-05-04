package com.ahumada.fuse.db.model;

import java.util.Date;

/**
 * DTO of VENTADOMICILIO_OWN.FV29_CLIENTE_DATOS
 * @author agnello
 *
 */
public class Fv29ClienteDatos {
	
	private String idTransaction, existe;
	private String docNumero, docTipo, docSerie, docEstado, docMotivo;
	private String nombres, apaterno, amaterno, nacionalidad, sexo, ecivil, fnacimiento, fdefuncion, fmatrimonio;
	private Date fechaConsultaDatos;
	private String faultString, faultDetail;
	
	/*
	 *  This parameter doesn't exists on database!
	 * 	We must populate this parameter with fechaConsultaDatos increased by the number of days the record is valid
	 */
	private Date fechaConsultaDatosValidez;
	private Date currentTimestampDatabase;
	
	/**
	 * Constructor with mandatory columns
	 * 
	 * @param docNumero
	 * @param docSerie
	 */
	public Fv29ClienteDatos(String docNumero, String docSerie) {
		this.docNumero = docNumero;
		this.docSerie =  docSerie;
	}
	
	/**
	 * Constructor for fault 
	 * 
	 * @param docNumero
	 * @param docSerie
	 * @param faultString
	 * @param faultDetail
	 */
	public Fv29ClienteDatos(String docNumero, String docSerie, String faultString, String faultDetail) {
		this.docNumero = docNumero;
		this.docSerie = docSerie;
		this.faultString = faultString;
		this.faultDetail = faultDetail;
	}
	
	
	public String getIdTransaction() {
		return idTransaction;
	}
	public void setIdTransaction(String idTransaction) {
		this.idTransaction = idTransaction;
	}
	public String getExiste() {
		return existe;
	}
	public void setExiste(String existe) {
		this.existe = existe;
	}
	public String getDocNumero() {
		return docNumero;
	}
	public void setDocNumero(String docNumero) {
		this.docNumero = docNumero;
	}
	public String getDocTipo() {
		return docTipo;
	}
	public void setDocTipo(String docTipo) {
		this.docTipo = docTipo;
	}
	public String getDocSerie() {
		return docSerie;
	}
	public void setDocSerie(String docSerie) {
		this.docSerie = docSerie;
	}
	public String getDocEstado() {
		return docEstado;
	}
	public void setDocEstado(String docEstado) {
		this.docEstado = docEstado;
	}
	public String getDocMotivo() {
		return docMotivo;
	}
	public void setDocMotivo(String docMotivo) {
		this.docMotivo = docMotivo;
	}
	public String getNombres() {
		return nombres;
	}
	public void setNombres(String nombres) {
		this.nombres = nombres;
	}
	public String getApaterno() {
		return apaterno;
	}
	public void setApaterno(String apaterno) {
		this.apaterno = apaterno;
	}
	public String getAmaterno() {
		return amaterno;
	}
	public void setAmaterno(String amaterno) {
		this.amaterno = amaterno;
	}
	public String getNacionalidad() {
		return nacionalidad;
	}
	public void setNacionalidad(String nacionalidad) {
		this.nacionalidad = nacionalidad;
	}
	public String getSexo() {
		return sexo;
	}
	public void setSexo(String sexo) {
		this.sexo = sexo;
	}
	public String getEcivil() {
		return ecivil;
	}
	public void setEcivil(String ecivil) {
		this.ecivil = ecivil;
	}
	public String getFnacimiento() {
		return fnacimiento;
	}
	public void setFnacimiento(String fnacimiento) {
		this.fnacimiento = fnacimiento;
	}
	public String getFdefuncion() {
		return fdefuncion;
	}
	public void setFdefuncion(String fdefuncion) {
		this.fdefuncion = fdefuncion;
	}
	public String getFmatrimonio() {
		return fmatrimonio;
	}
	public void setFmatrimonio(String fmatrimonio) {
		this.fmatrimonio = fmatrimonio;
	}
	public Date getFechaConsultaDatos() {
		return fechaConsultaDatos;
	}
	public void setFechaConsultaDatos(Date fechaConsultaDatos) {
		this.fechaConsultaDatos = fechaConsultaDatos;
	}

	public Date getFechaConsultaDatosValidez() {
		return fechaConsultaDatosValidez;
	}

	public void setFechaConsultaDatosValidez(Date fechaConsultaDatosValidez) {
		this.fechaConsultaDatosValidez = fechaConsultaDatosValidez;
	}

	public Date getCurrentTimestampDatabase() {
		return currentTimestampDatabase;
	}

	public void setCurrentTimestampDatabase(Date currentTimestampDatabase) {
		this.currentTimestampDatabase = currentTimestampDatabase;
	}

	public String getFaultString() {
		return faultString;
	}

	public void setFaultString(String faultString) {
		this.faultString = faultString;
	}

	public String getFaultDetail() {
		return faultDetail;
	}

	public void setFaultDetail(String faultDetail) {
		this.faultDetail = faultDetail;
	}
	
}
