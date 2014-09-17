package ru.barsopen.plsqlconverter.ast.typed;
public class record_var_dec implements record_declaration, _baseNode {
  public int _line = -1;
  public int _col = -1;
  public int _tokenStartIndex = -1;
  public int _tokenStopIndex = -1;
  public _baseNode _parent = null;
  public _baseNode _getParent() { return _parent; }
  public void _setParent(_baseNode value) { _parent = value; }
  public void _setBaseNode(_baseNode value) { this._parent = value; }
  public int _getLine() { return _line; }
  public int _getCol() { return _col; }
  public int _getTokenStartIndex() { return _tokenStartIndex; }
  public int _getTokenStopIndex() { return _tokenStopIndex; }
  public record_name record_name = null;
  public record_name get_record_name() { return this.record_name; }
  public void set_record_name(record_name value) {
    if (this.record_name != null) { this.record_name._setParent(null); }
    this.record_name = value;
    if (this.record_name != null) { this.record_name._setParent(this); }
  }
  public type_name type_name = null;
  public type_name get_type_name() { return this.type_name; }
  public void set_type_name(type_name value) {
    if (this.type_name != null) { this.type_name._setParent(null); }
    this.type_name = value;
    if (this.type_name != null) { this.type_name._setParent(this); }
  }
  public percent_type_or_rowtype percent_type_or_rowtype = null;
  public percent_type_or_rowtype get_percent_type_or_rowtype() { return this.percent_type_or_rowtype; }
  public void set_percent_type_or_rowtype(percent_type_or_rowtype value) {
    if (this.percent_type_or_rowtype != null) { this.percent_type_or_rowtype._setParent(null); }
    this.percent_type_or_rowtype = value;
    if (this.percent_type_or_rowtype != null) { this.percent_type_or_rowtype._setParent(this); }
  }

  public void _walk(_visitor visitor) {
    visitor.visit(this);
    if (this.record_name != null) {
      this.record_name._walk(visitor);
    }
    if (this.type_name != null) {
      this.type_name._walk(visitor);
    }
    if (this.percent_type_or_rowtype != null) {
      this.percent_type_or_rowtype._walk(visitor);
    }
  }
  public org.antlr.runtime.tree.Tree unparse() {
    org.antlr.runtime.CommonToken _token = new org.antlr.runtime.CommonToken(br.com.porcelli.parser.plsql.PLSQLParser.RECORD_VAR_DECLARE);
    _token.setLine(_line);
    _token.setCharPositionInLine(_col);
    _token.setText("RECORD_VAR_DECLARE");
    org.antlr.runtime.tree.CommonTree _result = new org.antlr.runtime.tree.CommonTree(_token);
    _result.setTokenStartIndex(_tokenStartIndex);
    _result.setTokenStopIndex(_tokenStopIndex);
    if (record_name == null) { throw new RuntimeException(); }
    _result.addChild(record_name.unparse());


    if (type_name == null) { throw new RuntimeException(); }
    _result.addChild(type_name.unparse());


    if (percent_type_or_rowtype == null) { throw new RuntimeException(); }
    _result.addChild(percent_type_or_rowtype.unparse());


    return _result;
  }

}
